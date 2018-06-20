package org.eso.ias.sink

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, ExecutorCompletionService, Executors, Future}

import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASValue, Identifier, IdentifierType}
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbFiles
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.pojos.IasioDao
import org.eso.ias.cdb.rdb.RdbReader
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosConsumer}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
 * The IasValueProcessor gets all the IasValues published in the BSDB
 * and sends them to the listener for further processing.
  *
  * @param processorIdentifier the idenmtifier of the value processor
  * @param kafkaBrokers the kafka brokers to connect to
  * @param listeners the processors of the IasValues read from the BSDB
  * @param iasioDaos The IASIOs configuration read from the CDB
 */
class IasValueProcessor(
                         val processorIdentifier: Identifier,
                         val kafkaBrokers: String,
                         val listeners: List[ValueListener],
                         val iasioDaos: Set[IasioDao]) extends IasioListener {
  require(Option(processorIdentifier).isDefined,"Invalid identifier")
  require(Option(listeners).isDefined && listeners.nonEmpty,"Mo listener defined")
  require(listeners.map(_.id).toSet.size==listeners.size,"Duplicated IDs of listeners")
  require(Option(iasioDaos).isDefined && iasioDaos.nonEmpty,"No IASIOs from CDB")

  /** The executor service to async process the IasValues in the listeners */
  val executorService = new ExecutorCompletionService[String](
    Executors.newFixedThreadPool(2 * listeners.size, new ProcessorThreadFactory(processorIdentifier.id)))

  /** The map of IasioDao by ID to pass to the listeners */
  val iasioDaosMap: Map[String,IasioDao] = iasioDaos.foldLeft(Map[String,IasioDao]()){ (z, dao) => z+(dao.getId -> dao)}

  /** The Kafka consumer of IasValues */
  val bsdbConsumer = new KafkaIasiosConsumer(kafkaBrokers, KafkaHelper.IASIOs_TOPIC_NAME, processorIdentifier.id)

  /** Signal if the processor has been closed */
  val closed = new AtomicBoolean(false)

  /** Signal if the processor has been initialized */
  val initialized = new AtomicBoolean(false)

  /** Signal if at least one thread of the processors is still running */
  val threadsRunning = new AtomicBoolean(false)

  /**
    * IasValues to process are buffered and sent to the listener when the size
    * of the buffer reached minSizeOfValsToProceesAtOnce size
    */
  val minSizeOfValsToProcessAtOnce: Int = Integer.getInteger(
    IasValueProcessor.sizeOfValsToProcPropName,
    IasValueProcessor.defaultSizeOfValsToProcess)
  IasValueProcessor.logger.info("Listeners will process {} IasValues at once",minSizeOfValsToProcessAtOnce)

  /**
    * Values received from the BSDB are saved in this list
    * until being processed by the listeners
    */
  val receivedIasValues: ListBuffer[IASValue[_]] = ListBuffer[IASValue[_]]()

  /**
    * The active listeners are those that are actively processing events.
    * When a listeners throws an exception, it is marked as broken and will stop
    * processing events
    *
    * @return the active (not broken) listeners
    */
  def activeListeners: List[ValueListener] = listeners.filterNot( _.isBroken)

  /**
    * @return The broken (i.e. not active) listeners
    */
  def brokenListeners: List[ValueListener] = listeners.filter(!_.isBroken)

  /**
    * @return true if there is at leat one active listener; false otherwise
    */
  def isThereActiveListener: Boolean = listeners.find(!_.isBroken).isDefined

  /**
    * Extends Callable[Unit] with the ID of the listener
    *
    * The call method returns the identifier of the listener if succeded.
    *
    * @param id the identifier of the listener
    * @param task the task of the listener to run
    */
  class IdentifiableCallable(val id: String, val task: Callable[Unit]) extends Callable[String] {
    override def call(): String = {
      task.call()
      id
    }
  }

  /**
    * Initialize the processor
    */
  def init(): Try[Unit] = {
    val alreadyInitialized=initialized.get()
    Try({
      if (alreadyInitialized) {
        IasValueProcessor.logger.warn("Processor {} already initialized",processorIdentifier.fullRunningID)
      } else {
        IasValueProcessor.logger.debug("Processor {} initializing",processorIdentifier.fullRunningID)
        // Init the kafka consumer
        IasValueProcessor.logger.debug("Initializing the BSDB consumer")
        bsdbConsumer.setUp()
        // Initialize the listeners
        IasValueProcessor.logger.debug("Initializing the listeners")
        initListeners()
        if (!isThereActiveListener) {
          throw new Exception("All the listeners failed to init")
        }
        // Start getting events from the BSDB
        IasValueProcessor.logger.debug("Start getting events...")
        bsdbConsumer.startGettingEvents(StartPosition.END,this)
        initialized.set(true)
        IasValueProcessor.logger.info("Processor {} initialized",processorIdentifier.fullRunningID)
      }
    })
  }

  /** Closes the processor */
  def close(): Unit = {
    val wasClosed = closed.getAndSet(true)
    if (wasClosed) {
      IasValueProcessor.logger.warn("Processor {} already closed",processorIdentifier.fullRunningID)
    } else {
      IasValueProcessor.logger.debug("Processor {} closing",processorIdentifier.fullRunningID)
      // Closes the Kafka consumer
      bsdbConsumer.tearDown()
      // Shut down the listeners
      closeListeners()
    }

  }

  /**
    * Notify the processors that new values has been read from the BSDB
    * and needs to be processed
    *
    * @param iasios The list if IASValues read from the BSDB
    */
  private def notifyListeners(iasios: List[IASValue[_]]): Unit = {
    require(Option(iasios).isDefined && iasios.nonEmpty)
    val callables: List[IdentifiableCallable] = activeListeners.map(listener => {
      val callable = new Callable[Unit]() {
        override def call(): Unit = listener.processIasValues(iasios)
      }
      new IdentifiableCallable(listener.id,callable)
    })
    sumbitTasks(callables)
  }

  /**
    * Concurrently submit the tasks and wait for their termination
    * return a list of ID of listeners that failed, if any
    *
    * @param callables the tasks to run concurrently
    * @return the list of IDs of listeners that threw an exception
    */
  private def sumbitTasks(callables: List[IdentifiableCallable]): List[String] = synchronized {
    require(callables.nonEmpty)

    // Check if there are still pending tasks
    assert(!threadsRunning.get())
    threadsRunning.set(true)

    // Concurrently run the callables
    callables.map(task => executorService.submit(task))

    // Wait for the termination of the threads
    val features: Seq[Future[String]] = for {
      i <- 1 to callables.length
      feature = executorService.poll()}  yield feature

    // Extract the IDs of the listeners that succeeded
    val idsOfListenersWhoSucceeded = features.foldLeft(List[String]()){ (z, feature) =>
        Try[String](feature.get()) match {
        case Success(id) => id::z
        case Failure(e) => IasValueProcessor.logger.error("Exception for processor",e)
                            z
      }

    }

    threadsRunning.set(false)

    // The list of IDs of the listener that failed nneds to be evaluated
    // indirectly because we have the IDs of the listener that succeded
    val allIds = callables.map(_.id)
    allIds.filterNot(id => idsOfListenersWhoSucceeded.contains(id))
  }

  /**
    * Initialize the listeners
    */
  private def initListeners(): Unit = {
    IasValueProcessor.logger.debug("Initializing the listeners")
    val callables: List[IdentifiableCallable] = activeListeners.map(listener => {
      val callable = new Callable[Unit]() {
        override def call(): Unit = listener.setUp(iasioDaosMap)
      }
      new IdentifiableCallable(listener.id,callable)
    })
    sumbitTasks(callables)
    IasValueProcessor.logger.debug("Listeners initialized")
  }

  /** Terminate the processors */
  private def closeListeners(): Unit = {
    IasValueProcessor.logger.debug("Closing the listeners")
    listeners.foreach(listener => {
      listener.tearDown()
    })

    IasValueProcessor.logger.info("Listeners closed")
  }

  /**
    * An IASIO has been read from the BSDB
    *
    * IASVales are initially grouped in the receivedIasValues
    *
    * @param iasio the IasValue read from the BSDB
    */
  override def iasioReceived (iasio: IASValue[_]): Unit = {
    assert(Option(iasio).isDefined)
    // Is there at least one processor alive?
    if (!isThereActiveListener) {
      IasValueProcessor.logger.error("No active processors remaining: shutting down")
      close()
    }
    receivedIasValues.append(iasio)
    if (receivedIasValues.length>minSizeOfValsToProcessAtOnce && !threadsRunning.get()) {
      val listOfIasValues = receivedIasValues.toList
      receivedIasValues.clear()
      notifyListeners(listOfIasValues)
    }
  }
}

object IasValueProcessor {
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[IasValueProcessor])

  /**
    * The default size of IasValues sent to listeners to process
    */
  val defaultSizeOfValsToProcess = 10

  /**
    * The name of the property to configure the
    * size of IasValues sent to listeners to process
    */
  val sizeOfValsToProcPropName = "org.eso.ias.valueprocessor.minsize"
  
  /** Build the usage message */
  def printUsage() = {
		"""Usage: IasValueProcessor Processor-ID [-jcdb JSON-CDB-PATH]
		-jcdb force the usage of the JSON CDB
		   * Processor-ID: the identifier of the IasValueProcessor
		   * JSON-CDB-PATH: the path of the JSON CDB"""
	}
  
  def main(args: Array[String]) = {
    require(!args.isEmpty, "Missing identifier in command line")
    require(args.size == 1 || args.size == 3, "Invalid command line params\n" + printUsage())
    require(if(args.size == 3) args(1)=="-jcdb" else true, "Invalid command line params\n" + printUsage())
    val processorId = args(0)
    // The identifier of the supervisor
    val identifier = new Identifier(processorId, IdentifierType.SINK, None)
    
    val reader: CdbReader = {
      if (args.size == 3) {
        logger.info("Using JSON CDB at {}",args(2))
        val cdbFiles: CdbFiles = new CdbJsonFiles(args(2))
        new JsonReader(cdbFiles)

      } else {
        logger.info("Using CDB RDB")
        new RdbReader()
      }
    }
    
    val kafkaBrokers = {
      // The brokers from the java property
      val fromPropsOpt=Option(System.getProperties().getProperty(KafkaHelper.BROKERS_PROPNAME))
      val iasDaoJOptional = reader.getIas
      val fromCdbOpt = if (iasDaoJOptional.isPresent) {
        Option(iasDaoJOptional.get().getBsdbUrl)
      } else {
        None
      }

      fromPropsOpt.getOrElse(fromCdbOpt.getOrElse(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS))
    }
  }
}