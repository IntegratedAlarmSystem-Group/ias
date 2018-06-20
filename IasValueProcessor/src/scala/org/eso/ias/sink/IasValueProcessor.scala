package org.eso.ias.sink

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, ExecutorCompletionService, Executors, Future}

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.dasu.subscriber.{InputSubscriber, InputsListener}
import org.eso.ias.heartbeat.{HbEngine, HbProducer, HeartbeatStatus}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASValue, Identifier}

import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
 * The IasValueProcessor gets all the IasValues published in the BSDB
 * and sends them to the listener for further processing.
  *
  * @param processorIdentifier the idenmtifier of the value processor
  * @param listeners the processors of the IasValues read from the BSDB
  * @param hbProducer The HB generator
  * @param inputsSubscriber The subscriber to get events from the BDSB
  * @param cdbReader The CDB reader
 */
class IasValueProcessor(
                         val processorIdentifier: Identifier,
                         val listeners: List[ValueListener],
                         private val hbProducer: HbProducer,
                         private val inputsSubscriber: InputSubscriber,
                         cdbReader: CdbReader) extends InputsListener {
  require(Option(processorIdentifier).isDefined,"Invalid identifier")
  require(Option(listeners).isDefined && listeners.nonEmpty,"Mo listeners defined")
  require(listeners.map(_.id).toSet.size==listeners.size,"Duplicated IDs of listeners")
  require(Option(hbProducer).isDefined,"Invalid HB producer")
  require(Option(inputsSubscriber).isDefined,"Invalid inputs subscriber")
  require(Option(cdbReader).isDefined,"Invalid CDB reader")

  /** The executor service to async process the IasValues in the listeners */
  val executorService = new ExecutorCompletionService[String](
    Executors.newFixedThreadPool(2 * listeners.size, new ProcessorThreadFactory(processorIdentifier.id)))

  /** The configuration of IASIOs from the CDB */
  val iasioDaos: List[IasioDao] = {
    val temp: util.Set[IasioDao] = cdbReader.getIasios.orElseThrow(() => new IllegalArgumentException("IasDaos not found in CDB"))
    JavaConverters.asScalaSet(temp).toList
  }

  /** The map of IasioDao by ID to pass to the listeners */
  val iasioDaosMap: Map[String,IasioDao] = iasioDaos.foldLeft(Map[String,IasioDao]()){ (z, dao) => z+(dao.getId -> dao)}

  /** The heartbeat Engine */
  val hbEngine: HbEngine = {
    val iasDao: IasDao = cdbReader.getIas.orElseThrow(() => new IllegalArgumentException("IasDao not found"))
    HbEngine(processorIdentifier.fullRunningID,iasDao.getHbFrequency,hbProducer)
  }

  // Closes the CDB reader
  cdbReader.shutdown()

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
  def isThereActiveListener: Boolean = listeners.exists(!_.isBroken)

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
        // Start the HB
        hbEngine.start(HeartbeatStatus.STARTING_UP)
        // Init the kafka consumer
        IasValueProcessor.logger.debug("Initializing the BSDB consumer")
        inputsSubscriber.initializeSubscriber() match {
          case Failure(e) => throw new Exception("Error initilizing the subscriber",e)
          case Success(_) =>
        }
        // Initialize the listeners
        IasValueProcessor.logger.debug("Initializing the listeners")
        initListeners()
        if (!isThereActiveListener) {
          throw new Exception("All the listeners failed to init")
        }
        // Start getting events from the BSDB
        IasValueProcessor.logger.debug("Start getting events...")
        inputsSubscriber.startSubscriber(this,Set.empty)
        initialized.set(true)
        hbEngine.updateHbState(HeartbeatStatus.RUNNING)
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
      hbEngine.updateHbState(HeartbeatStatus.EXITING)
      // Closes the Kafka consumer
      inputsSubscriber.cleanUpSubscriber()
      // Shut down the listeners
      closeListeners()
      // Stop the HB
      hbEngine.shutdown()
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
    * @param iasios the IasValues read from the BSDB
    */
  override def inputsReceived(iasios: Set[IASValue[_]]): Unit = {
    assert(Option(iasios).isDefined)
    // Is there at least one processor alive?
    if (!isThereActiveListener) {
      IasValueProcessor.logger.error("No active processors remaining: shutting down")
      close()
    }

    // Discard the IASIOs not defined in the CDB
    iasios.foreach(iasio => {
      if (iasioDaosMap.get(iasio.id).isDefined) {
        receivedIasValues.append(iasio)
      } else {
        IasValueProcessor.logger.warn("The CDB does not contain a IAS value with ID {}: value discarded",iasio.id)
      }
    })

    if (receivedIasValues.length>minSizeOfValsToProcessAtOnce && !threadsRunning.get()) {
      val listOfIasValues = receivedIasValues.toList
      receivedIasValues.clear()
      notifyListeners(listOfIasValues)
    }
  }
}

object IasValueProcessor {
  
  /** The logger */
  val logger: Logger = IASLogger.getLogger(classOf[IasValueProcessor])

  /**
    * The default size of IasValues sent to listeners to process
    */
  val defaultSizeOfValsToProcess = 10

  /**
    * The name of the property to configure the
    * size of IasValues sent to listeners to process
    */
  val sizeOfValsToProcPropName = "org.eso.ias.valueprocessor.minsize"

}