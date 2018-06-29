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
  * In this version the IasValueProcessor does not take any action if
  * one of the listeners is too slow apart of logging messages.
  * The slowness is detected when the queue of received and
  * not yet proocessed values grows too much. In this case the
  * IasValueProcessor logs a warning. To avoid submitting
  * too many logs, the message is logged with a throttling.
  *
  * The buffer is bounded by maxBufferSize: if threads do not consume
  * values fast enough the oldest values in the buffer are removed to avoid
  * out of memory.
  *
  * The IasValueProcessor monitors the termination time of the threads
  * and kill threads that do not terminate in killThreadAfter seconds.
  * To kill a thread, its close method is invoked and it will be removed
  * from the active listener.
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
                         val iasDao: IasDao,
                         val iasioDaos: List[IasioDao]) extends InputsListener {
  require(Option(processorIdentifier).isDefined,"Invalid identifier")
  require(Option(listeners).isDefined && listeners.nonEmpty,"Mo listeners defined")
  require(listeners.map(_.id).toSet.size==listeners.size,"Duplicated IDs of listeners")
  require(Option(hbProducer).isDefined,"Invalid HB producer")
  require(Option(inputsSubscriber).isDefined,"Invalid inputs subscriber")
  require(Option(iasDao).isDefined,"Invalid IAS configuration")
  require(Option(iasioDaos).isDefined && iasioDaos.nonEmpty,"Invalid configuration of IASIOs from CDB")
  require(processorIdentifier.idType==IdentifierType.SINK,"Identifier tyope should be SINK")

  IasValueProcessor.logger.info("{} processors will work on IAsValues read from the BSDB",listeners.length)

  /** The thread factory for the executors */
  val threadFactory = new ProcessorThreadFactory(processorIdentifier.id)

  /** The executor service to async process the IasValues in the listeners */
  val executorService = new ExecutorCompletionService[String](
    Executors.newFixedThreadPool(2 * listeners.size, threadFactory))

  /** The periodic executor for periodic processing of values */
  val periodicScheduledExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(1,threadFactory)

  /**
    * The point in time when the values has been proccessed
    * for the last time
    */
  val lastProcessingTime = new AtomicLong(0)

  /** The map of IasioDao by ID to pass to the listeners */
  val iasioDaosMap: Map[String,IasioDao] = iasioDaos.foldLeft(Map[String,IasioDao]()){ (z, dao) => z+(dao.getId -> dao)}

  /** The heartbeat Engine */
  val hbEngine: HbEngine = HbEngine(processorIdentifier.fullRunningID,iasDao.getHbFrequency,hbProducer)

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

  /**
    * If the size of the buffer is greater than bufferSizeThreshold
    * the processor emits a warning because the listener are too slow
    * processing vales read from the BSDB
    */
  val bufferSizeThreshold: Integer = Integer.getInteger(
    IasValueProcessor.bufferSizeWarnThresholdPropName,
    IasValueProcessor.bufferSizeWarnThresholdDefault)

  /**
    * A log to warn about the size of the buffer
    * is submitted only if the the last one was published
    * more then logThrottlingTime milliseconds before
    */
  val logThrottlingTime: Long = Integer.getInteger(
    IasValueProcessor.logThrottlingTimePropName,
    IasValueProcessor.logThrottlingTimeDefault)*1000

  /** The point in time when the last warning log has been submitted */
  val lastSubmittedWarningTime = new AtomicLong(0)

  /** The number of warning messages suppressed by the throttling */
  val suppressedWarningMessages = new AtomicLong(0)

  /**
    * Timeout (secs) waiting for termination of threads: if a timeout elapses a log is issued
    * reporting the name of threads that did not yet terminate for investigation
    */
  val timeoutWaitingThreadsTermination: Int = Integer.getInteger(
    IasValueProcessor.threadWaitTimoutPropName,
    IasValueProcessor.threadWaitTimoutDefault)
  IasValueProcessor.logger.info("Timeout for thread termination set to {}",timeoutWaitingThreadsTermination)

  /**
    * The max allowed size of the buffer of received and not yet processed IASValues (receivedIasValues):
    * if the buffer grows over this limit, oldest values are removed
    */
  val maxBufferSize: Int = Integer.getInteger(IasValueProcessor.maxBufferSizePropName,IasValueProcessor.maxBufferSizeDefault)
  IasValueProcessor.logger.info("Max size of buffer of not processed values = {}",maxBufferSize)

  /**
    * Kill threads that do not terminate in killThreadAfter seconds
    */
  val killThreadAfter: Int = Integer.getInteger(
    IasValueProcessor.killThreadAfterPropName,
    IasValueProcessor.killThreadAfterDefault)
  IasValueProcessor.logger.info("Will kill threads that do not terminate in {} seconds",killThreadAfter)

  /** The hook for a clean shutdown */
  val shutdownHookThread: Thread = new Thread() {
    override def run(): Unit =  close()
  }

  IasValueProcessor.logger.debug("{} processor built",processorIdentifier.id)

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
  def brokenListeners: List[ValueListener] = listeners.filter(_.isBroken)

  /**
    * @return true if there is at leat one active listener; false otherwise
    */
  def isThereActiveListener: Boolean = listeners.exists(!_.isBroken)

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
        periodicScheduledExecutor.scheduleWithFixedDelay(() =>
          notifyListeners(), periodicProcessingTime,periodicProcessingTime,TimeUnit.MILLISECONDS)

        Runtime.getRuntime.addShutdownHook(shutdownHookThread)

        IasValueProcessor.logger.info("Processor {} initialized",processorIdentifier.fullRunningID)
      }
    })
  }

  /** Closes the processor */
  def close(): Unit = {
    val wasClosed = closed.getAndSet(true)
    if (initialized.get()) {
      Runtime.getRuntime.removeShutdownHook(shutdownHookThread)
    }
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

      // Is the size of the buffer growing too fast?
      // Log a message (with throttling)
      if (receivedIasValues.length>bufferSizeThreshold) {
        if (System.currentTimeMillis() - lastSubmittedWarningTime.get() > logThrottlingTime) {
          IasValueProcessor.logger.warn(
            "Too many values ({}) to process. Is any of the the processors ({}) too slow? ({} similar messsages hidden in the past {} msecs)",
            receivedIasValues.length,
            activeListeners.map(_.id).mkString(","),
            suppressedWarningMessages.getAndSet(0),
            logThrottlingTime)
        } else {
          suppressedWarningMessages incrementAndGet()
        }
      }

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

    /**
      * kill the threads with the passed ID
      *
      * Killing is done by calling their close method: if the listener is well
      * written it should notice the termination and cleanly terminate
      *
      * @param startTime the point in time when threads have been started
      * @param threadIds The ids of the threads to terminate
      * @return the number of threads terminated
      */
    def killThreads(startTime: Long, threadIds: List[String]): Int = {
      require(Option(threadIds).isDefined && threadIds.nonEmpty)
      val listenersToKill = listeners.filter(l => threadIds.contains(l.id))
      listenersToKill.foreach(processor => {
        IasValueProcessor.logger.error("Terminating slow processor {}",processor.id)
        processor.markAsBroken()
        Try(processor.tearDown()) match {
          case Success(_) => IasValueProcessor.logger.info("Process {} successfully closed",processor.id)
          case Failure(e) => IasValueProcessor.logger.warn("Processor {} terminated with exception {}",
            processor.id,
            e.getMessage)
        }

      })
      listenersToKill.length
    }

    require(callables.nonEmpty)

    assert(!threadsRunning.get())
    threadsRunning.set(true)

    // The IDs of all the listeners to run
    val allIds: List[String] = activeListeners.map(_.id)

    IasValueProcessor.logger.debug("Submitting {} tasks for {}",
      callables.length.toString,
      activeListeners.map(_.id).mkString(","))
    assert(callables.length==allIds.length)

    // Concurrently run the callables
    val startProcessingTime = System.currentTimeMillis()
    val submittedFeatures = callables.map(task => executorService.submit(task))

    // Wait for the termination of the threads
    IasValueProcessor.logger.debug("Waiting for termination of {} tasks",submittedFeatures.length)
    val idsOfProcessorsWhoSucceeded = ListBuffer[String]()
    var terminatedProcs = 0
    while(terminatedProcs<callables.length && !closed.get()) {
      val tryOptFuture = Try[Option[Future[String]]](Option(executorService.poll(timeoutWaitingThreadsTermination,TimeUnit.SECONDS)))
      tryOptFuture match {
        case Success(Some(future)) => // The thread termonated with or without exception
          terminatedProcs = terminatedProcs + 1
          Try(future.get()) match {
            case Success(id) => idsOfProcessorsWhoSucceeded.append(id)
            case Failure(procExc) => IasValueProcessor.logger.error("Exception for processor",procExc)
          }
        case Success(None) => // No thread terminated: timeout!
          val notTerminatedThreadIds = allIds.filterNot(id => idsOfProcessorsWhoSucceeded.contains(id))
          IasValueProcessor.logger.warn("Slow processors detected: {} did not terminate in {} seconds",
            notTerminatedThreadIds.mkString(","),
            (System.currentTimeMillis()-startProcessingTime)/1000)
          if (System.currentTimeMillis()-startProcessingTime>killThreadAfter*1000) {
            IasValueProcessor.logger.info("Going to terminate {} processors",notTerminatedThreadIds.mkString(","))
            terminatedProcs = terminatedProcs + killThreads(startProcessingTime,notTerminatedThreadIds)
          }
        case Failure(e) => // InterruptedException => nothing to do
      }
    }

    threadsRunning.set(false)

    // The list of IDs of the listener that failed nneds to be evaluated
    // indirectly because we have the IDs of the listener that succeded
    allIds.filterNot(id => idsOfProcessorsWhoSucceeded.contains(id))
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
    } else if (receivedIasValues.length>maxBufferSize) {
      val numOfValuesTodiscard = receivedIasValues.length-maxBufferSize
      receivedIasValues.remove(0,numOfValuesTodiscard)
      IasValueProcessor.logger.warn("Max size of buffer reached: {} values discarded",numOfValuesTodiscard)
    }
  }
}

object IasValueProcessor {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(classOf[IasValueProcessor])

  /**
    * The default min size of IasValues sent to listeners to process
    */
  val defaultSizeOfValsToProcess = 50

  /**
    * The name of the property to configure the
    * size of IasValues sent to listeners to process
    */
  val sizeOfValsToProcPropName = "org.eso.ias.valueprocessor.thread.minsize"

  /**
    * The processing of IASValues is triggered every
    * forceSendingTimeInterval msecs if there are no new inputs
    */
  val defaultPeriodicSendingTimeInterval = 500

  /**
    * The name of the property to change periodic processing of IASValue
    */
  val periodicSendingTimeIntervalPropName = "org.eso.ias.valueprocessor.thread.periodic.time"

  /**
    * The default value for the max allowd size of the buffer
    */
  val maxBufferSizeDefault = 100000

  /**
    * The name of the property to customize the max size of the buffer
    */
  val maxBufferSizePropName = "org.eso.ias.valueprocessor.maxbufsize"

  /** The time (seconds) to wait for termination of one thread */
  val threadWaitTimoutDefault = 3

  /** The name of the java property top customize the time to wait for termination of one thread */
  val threadWaitTimoutPropName = "org.eso.ias.valueprocessor.thread.timeout"

  /** Kills thread that do not terminate in this number of seconds */
  val killThreadAfterDefault = 60

  /** Java property to customize the time to kill non responding threads */
  val killThreadAfterPropName = "org.eso.ias.valueprocessor.thread.killafter"

  /** Interval of time (seconds) between consecutive identical logs */
  val logThrottlingTimeDefault = 5

  /** The name of the java property to customize the interval of time (seconds) between consecutive identical logs */
  val logThrottlingTimePropName = "org.eso.ias.valueprocessor.log.throttling"

  /** The size of the buffer to log warnings */
  val bufferSizeWarnThresholdDefault = 25000

  val bufferSizeWarnThresholdPropName = "org.eso.ias.valueprocessor.log.warningbuffersize"

}