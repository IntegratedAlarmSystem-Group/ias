package org.eso.ias.sink

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent._

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.dasu.subscriber.{InputSubscriber, InputsListener}
import org.eso.ias.heartbeat.{HbEngine, HbProducer, HeartbeatStatus}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASValue, Identifier, IdentifierType}

import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
  * The IasValueProcessor gets all the IasValues published in the BSDB
  * and sends them to the listener for further processing.
  *
  * The processing is triggered when the buffer receivedValues contains
  * at least minSizeOfValsToProcessAtOnce itemes.
  * It is also periodically triggered every periodicProcessingTime msecs.
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
  require(processorIdentifier.idType==IdentifierType.SINK,"Identifier tyope should be SINK")

  IasValueProcessor.logger.info("{} processors will work on IAsValues read from the BSDB",listeners.length)

  /** The thread factory for the executors */
  val threadFactory = new ProcessorThreadFactory(processorIdentifier.id)

  /** The executor service to async process the IasValues in the listeners */
  val executorService = new ExecutorCompletionService[String](
    Executors.newFixedThreadPool(2 * listeners.size, threadFactory))

  /** The periodic executor for periodic processing of values */
  val periodicScheduledExecutor = Executors.newScheduledThreadPool(1,threadFactory)

  /**
    * The point in time when the values has been proccessed
    * for the last time
    */
  val lastProcessingTime = new AtomicLong(0)

  /** The configuration of the IAS read from the CDB */
  val iasDao = cdbReader.getIas.orElseThrow(()=> new IllegalArgumentException("IasDao not found in CDB"))

  /** The configuration of IASIOs from the CDB */
  val iasioDaos: List[IasioDao] = {
    val temp: util.Set[IasioDao] = cdbReader.getIasios.orElseThrow(() => new IllegalArgumentException("IasDaos not found in CDB"))
    JavaConverters.asScalaSet(temp).toList
  }
  IasValueProcessor.logger.info("{} IASIO found in CDB",iasioDaos.length)

  /** The map of IasioDao by ID to pass to the listeners */
  val iasioDaosMap: Map[String,IasioDao] = iasioDaos.foldLeft(Map[String,IasioDao]()){ (z, dao) => z+(dao.getId -> dao)}

  /** The heartbeat Engine */
  val hbEngine: HbEngine = HbEngine(processorIdentifier.fullRunningID,iasDao.getHbFrequency,hbProducer)

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
  IasValueProcessor.logger.info("Listeners will buffer {} IasValues before processing",minSizeOfValsToProcessAtOnce)

  val periodicProcessingTime: Int = Integer.getInteger(
    IasValueProcessor.periodicSendingTimeIntervalPropName,
    IasValueProcessor.defaultPeriodicSendingTimeInterval)
  IasValueProcessor.logger.info("Listeners will process IasValues every {}ms",periodicProcessingTime)

  /**
    * Values received from the BSDB are saved in this list
    * until being processed by the listeners
    */
  val receivedIasValues: ListBuffer[IASValue[_]] = ListBuffer[IASValue[_]]()

  IasValueProcessor.logger.debug("{} processor built",processorIdentifier.id)

  /**
    * The active listeners are those that are actively processing events.
    * When a listeners throws an exception, it is marked as broken and will stop
    * processing events
    *
    * @return the active (not broken) listeners
    */
  def activeListeners(): List[ValueListener] = listeners.filterNot( _.isBroken)

  /**
    * @return The broken (i.e. not active) listeners
    */
  def brokenListeners(): List[ValueListener] = listeners.filter(_.isBroken)

  /**
    * @return true if there is at leat one active listener; false otherwise
    */
  def isThereActiveListener(): Boolean = listeners.exists(!_.isBroken)

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
        // Start the periodic processing
        periodicScheduledExecutor.scheduleWithFixedDelay(new Runnable {
            override def run(): Unit = notifyListeners()
          }, periodicProcessingTime,periodicProcessingTime,TimeUnit.MILLISECONDS)
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
      // Stops the periodic processing
      periodicScheduledExecutor.shutdown()
      if (!periodicScheduledExecutor.awaitTermination(10,TimeUnit.SECONDS)) {
        IasValueProcessor.logger.warn("Periodic task dis not terminate in time")
      }
      // Stop the HB
      hbEngine.shutdown()
    }

  }

  /**
    * Notify the processors that new values has been read from the BSDB
    * and needs to be processed
    */
  private def notifyListeners(): Unit = synchronized {
    if (
        !closed.get() &&
        receivedIasValues.nonEmpty &&
        System.currentTimeMillis()-lastProcessingTime.get()>periodicProcessingTime) {
      val iasios = receivedIasValues.toList
      val callables: List[Callable[String]] = activeListeners.map(listener =>
        new Callable[String]() {
          override def call(): String = listener.processIasValues(iasios)
        }
      )
      receivedIasValues.clear()
      // Submit for parallel processing
      sumbitTasks(callables)
      lastProcessingTime.set(System.currentTimeMillis())
    }
  }

  /**
    * Concurrently submit the tasks and wait for their termination
    * return a list of ID of listeners that failed, if any
    *
    * @param callables the tasks to run concurrently
    * @return the list of IDs of listeners that threw an exception
    */
  private def sumbitTasks(callables: List[Callable[String]]): List[String] = synchronized {
    require(callables.nonEmpty)

    assert(!threadsRunning.get())
    threadsRunning.set(true)

    // The IDs of all the listeners to run
    val allIds: List[String] = activeListeners().map(_.id)

    IasValueProcessor.logger.debug("Submitting {} tasks for {}",
      callables.length.toString,
      activeListeners.map(_.id).mkString(","))
    assert(callables.length==allIds.length)

    // Concurrently run the callables
    var submittedFeatures = callables.map(task => executorService.submit(task))

    // Wait for the termination of the threads
    IasValueProcessor.logger.debug("Waiting for termination of {} tasks",submittedFeatures.length)
    val features: Seq[Future[String]] = for {
      i <- 1 to callables.length
      feature = executorService.take()}  yield feature
    IasValueProcessor.logger.debug("Tasks terminated")

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
    allIds.filterNot(id => idsOfListenersWhoSucceeded.contains(id))
  }

  /**
    * Initialize the listeners
    */
  private def initListeners(): Unit = {
    IasValueProcessor.logger.debug("Initializing the listeners")
    val callables: List[Callable[String]] = activeListeners.map(listener =>
      new Callable[String]() {
        override def call(): String = listener.setUp(iasDao,iasioDaosMap)
      }
    )
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

  /** Processes the values accumulated in the past time interval  */
  private def periodicProcessTask() = synchronized {
    IasValueProcessor.logger.debug("Periodic processing")
  }

  /**
    * An IASIO has been read from the BSDB
    *
    * IASVales are initially grouped in the received IasValues
    *
    * @param iasios the IasValues read from the BSDB
    */
  override def inputsReceived(iasios: Set[IASValue[_]]): Unit = synchronized {
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
      notifyListeners()
    }
  }
}

object IasValueProcessor {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(classOf[IasValueProcessor])

  /**
    * The default min size of IasValues sent to listeners to process
    */
  val defaultSizeOfValsToProcess = 25

  /**
    * The name of the property to configure the
    * size of IasValues sent to listeners to process
    */
  val sizeOfValsToProcPropName = "org.eso.ias.valueprocessor.minsize"

  /**
    * The processing of IASValues is triggered every
    * forceSendingTimeInterval msecs if there are no new inputs
    */
  val defaultPeriodicSendingTimeInterval = 500

  /**
    * The name of the property to change periodic processing of IASValue
    */
  val periodicSendingTimeIntervalPropName = "org.eso.ias.valueprocessor.periodic.process.time"

}