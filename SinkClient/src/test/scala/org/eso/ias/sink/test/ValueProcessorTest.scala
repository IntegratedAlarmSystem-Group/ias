package org.eso.ias.sink.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.{IasTypeDao, IasioDao, TemplateDao}
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.command.{CommandListener, CommandManager}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{HbMsgSerializer, HbProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.{IasValueProcessor, ValueListener}
import org.eso.ias.types.*
import org.eso.ias.types.IasValidity.UNRELIABLE
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.FileSystems
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters

// The following import is required by the usage of the fixture
import scala.language.reflectiveCalls

/**
  * The listener for testing
  *
  * @param id The identifier to distinguish between many listeners int he same processor
  *           Mainly used for logging messages
  * @param throwExcInit if true the init method throws an exception
  * @param throwExcProcess if true the process method throws an exception
  * @param timesOut if >0 the process methods trigger a timeout after timesOut seconds
  */
class ListenerForTest(
                       id: String,
                       throwExcInit: Boolean=false,
                       throwExcProcess: Boolean=false,
                       timesOut: Int = 0
                     )
  extends ValueListener(id) {

  /** The logger */
  val logger = IASLogger.getLogger(classOf[ListenerForTest])

  val callstoUserDefinedClose = new AtomicInteger(0)

  val callstoUserDefinedInit = new AtomicInteger(0)

  val callstoUserDefinedProcess = new AtomicInteger(0)

  /** The values received from the BSDB and to be processed */
  val receivedValues = ListBuffer[IASValue[?]]()

  override def close(): Unit = {
    callstoUserDefinedClose.incrementAndGet()
    logger.info("Listener {} closed",id)
  }

  override def init(): Unit = {
    callstoUserDefinedInit.incrementAndGet()
    if (throwExcInit) {
      logger.info("Listener {} is throwing exception in init()",id)
      throw new Exception("Exception for testing")
    } else {
      logger.info("Listener {} initilized",id)
    }

  }

  override def process(iasValues: List[IASValue[?]]): Unit = {
    callstoUserDefinedProcess.incrementAndGet()
    if (timesOut>0) {
      Thread.sleep(timesOut*1000)
    } else if (throwExcProcess) {
      logger.info("Listener {} is throwing exception in process()",id)
      throw new Exception("Exception for testing")
    } else {
      receivedValues.appendAll(iasValues)
      logger.info("Listener {} received {} values to process", id, iasValues.length)
    }
  }
}

class CommandManagerTest(id: String) extends CommandManager(id) {

  override def close(): Unit = {}

  override def start(commandListener: CommandListener, closeable: AutoCloseable): Unit = {}

}

class HbProducerTest(s: HbMsgSerializer) extends HbProducer(s) {
  /** The logger */
  val logger = IASLogger.getLogger(classOf[HbProducerTest])

  val inited = new AtomicBoolean(false)
  val closed = new AtomicBoolean(false)
  val numOfHBs = new AtomicInteger(0)

  /** Initialize the producer */
  override def init() = { inited.set(true)}

  /** Shutdown the producer */
  override def shutdown() = { closed.set(true)}

  /** Push the string */
  override def push(hbAsString: String): Unit = {
    logger.info("HeartBeat [{}]",hbAsString)
    numOfHBs.incrementAndGet()
  }
}

/** Test the IasValueProcessor */
class ValueProcessorTest extends AnyFlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(classOf[ValueProcessorTest])

  /** Fixture to build same type of objects for the tests */
  trait Fixture {
    /** The identifier */
    val processorIdentifier = "ProcessorTestID"

      // Build the listeners
    val listeners: List[ListenerForTest] = {
      for {
          i <- 1 to 5
          id = s"ListenerID-$i"
          listener = new ListenerForTest(id)
        } yield listener
      }.toList

    val inputsProvider: DirectInputSubscriber = new DirectInputSubscriber()

    // Build the CDB reader
    val cdbParentPath = FileSystems.getDefault().getPath("src/test");
    val cdbReader: CdbReader = new StructuredTextReader(cdbParentPath.toFile)
    cdbReader.init()

    val iasDao = {
      val iasDaoJOpt = cdbReader.getIas
      assert(iasDaoJOpt.isPresent, "Error getting the IAS from the CDB")
      iasDaoJOpt.get()
    }

    val iasiosDaos: List[IasioDao] = {
      val iasiosDaoJOpt = cdbReader.getIasios()
      assert(iasiosDaoJOpt.isPresent, "Error getting the IASIOs from the CDB")
      CollectionConverters.asScala(iasiosDaoJOpt.get()).toList
    }
    cdbReader.shutdown()

    val templateDao = new TemplateDao("TemplToTestInputs",1,25)

    /** The processor to test with no failing processors */
    val processor: IasValueProcessor  = new IasValueProcessor(
      processorIdentifier,
      listeners,
      None,
      Option(new HbProducerTest(new HbJsonSerializer())),
      Option(new CommandManagerTest(processorIdentifier)),
      Option(inputsProvider),
      iasDao,
      iasiosDaos,
      List(templateDao))

    val inputsProviderWithFailures: DirectInputSubscriber = new DirectInputSubscriber()

    // The listeners with failures
    val idOfFailingInit = "ListenerThatFailIniting"
    val idOfFailinProcess = "ListenerThatFailProcessing"
    val listenersWithFailure = new ListenerForTest(idOfFailingInit,throwExcInit = true)::
      new ListenerForTest(idOfFailinProcess,throwExcProcess = true) ::
      listeners

    /** The identifier for the processor with timeout */
    val processorIdentifierWF = "ProcessorTestID-WithFailures"

    /** The processor to test with failing processors */
    val processorWithFailures: IasValueProcessor  = new IasValueProcessor(
      processorIdentifierWF,
      listenersWithFailure,
      None,
      Option(new HbProducerTest(new HbJsonSerializer())),
      Option(new CommandManagerTest(processorIdentifierWF)),
      Option(inputsProviderWithFailures),
      iasDao,
      iasiosDaos,
      List.empty[TemplateDao])

    // Set the timeout of the processor
    val timeout = 10
    System.getProperties.put(IasValueProcessor.killThreadAfterPropName,timeout.toString)

    val inputsProviderWithTO: DirectInputSubscriber = new DirectInputSubscriber()

    // The listeners with timeout
    val idOfFailingTO = "ListenerThatTimesOut"
    val listenersWithTO = new ListenerForTest(idOfFailingTO,timesOut = timeout+5)::listeners

    /** The identifier for the processor with timeout */
    val processorIdentifierTO = "ProcessorTestID-WithFailures"

    /** The processor to test with timeout */
    val processorWithTO: IasValueProcessor  = new IasValueProcessor(
      processorIdentifierTO,
      listenersWithTO,
      None,
      Option(new HbProducerTest(new HbJsonSerializer())),
      Option(new CommandManagerTest(processorIdentifierTO)),
      Option(inputsProviderWithTO),
      iasDao,
      iasiosDaos,
      List.empty[TemplateDao])

    }

  /**
    * Build a IASValue to send to the processor
    *
    * @param id The identifier of the monitor point
    * @param d the value
    * @return the IASValue
    */
  def buildValue(id: String, d: Alarm): IASValue[?] = {

    // The identifier of the monitored system
    val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

    // The identifier of the plugin
    val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))

    // The identifier of the converter
    val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

    // The ID of the monitor point
    val inputId = new Identifier(id, IdentifierType.IASIO,converterId)

    val t0 = System.currentTimeMillis()-100

    IASValue.build(
      d,
      OperationalMode.OPERATIONAL,
      UNRELIABLE,
      inputId.fullRunningID,
      IASTypes.ALARM,
      t0,
      t0+1,
      t0+5,
      t0+10,
      t0+15,
      null,
      null,
      null,
      null)
  }

  behavior of "The value processor"

  it must "not init and close at startup"  in new Fixture {
    assert(!processor.closed.get())
    assert(!processor.initialized.get())
  }

  it must "init all the listeners"  in new Fixture {
    processor.init()
    assert(!processor.closed.get())
    assert(processor.initialized.get())

    assert(processor.listeners.forall(_.initialized.get()))
    assert(processor.listeners.forall(!_.isBroken))

    assert(listeners.forall(_.callstoUserDefinedClose.get()==0))
    assert(listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))

    logger.info("Active listeners {}",processor.activeListeners.map(_.id).mkString(","))
    assert(processor.activeListeners.length==listeners.length)
    logger.info("broken listeners {}",processor.brokenListeners.map(_.id).mkString(","))
    assert(processor.brokenListeners.isEmpty)
    assert(processor.isThereActiveListener)
    processor.close()
  }

  it must "does not init the listeners more then once"  in new Fixture {
    processor.init()
    processor.init()
    assert(!processor.closed.get())
    assert(processor.initialized.get())

    assert(processor.listeners.forall(_.initialized.get()))
    assert(processor.listeners.forall(!_.isBroken))

    assert(listeners.forall(_.callstoUserDefinedClose.get()==0))
    assert(listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))

    logger.info("Active listeners {}",processor.activeListeners.map(_.id).mkString(","))
    assert(processor.activeListeners.length==listeners.length)
    logger.info("broken listeners {}",processor.brokenListeners.map(_.id).mkString(","))
    assert(processor.brokenListeners.isEmpty)
    assert(processor.isThereActiveListener)
    processor.close()
  }
  it must "init the HB"  in new Fixture {
    processor.init()
    assert(processor.hbEngine.isStarted)
    processor.close()
  }

  it must "init the producer"  in new Fixture {
    processor.init()
    assert(inputsProvider.isInitialized)
    processor.close()
  }


  it must "close all the listeners"  in new Fixture {
    processor.init()
    processor.close()
    assert(processor.closed.get())
    assert(processor.initialized.get())

    assert(processor.listeners.forall(_.initialized.get()))
    assert(processor.listeners.forall(!_.isBroken))
    assert(processor.listeners.forall(_.closed.get()))
    assert(processor.listeners.forall(!_.isBroken))

    assert(listeners.forall(_.callstoUserDefinedClose.get()==1))
    assert(listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))

    assert(processor.activeListeners.length==listeners.length)
    assert(processor.brokenListeners.isEmpty)
    assert(processor.isThereActiveListener)
  }

  it must "close all the listeners only once"  in new Fixture {
    processor.init()
    processor.close()
    processor.close()
    assert(processor.closed.get())
    assert(processor.initialized.get())

    assert(processor.listeners.forall(_.initialized.get()))
    assert(processor.listeners.forall(!_.isBroken))
    assert(processor.listeners.forall(_.closed.get()))
    assert(processor.listeners.forall(!_.isBroken))

    assert(listeners.forall(_.callstoUserDefinedClose.get()==1))
    assert(listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))

    assert(processor.activeListeners.length==listeners.length)
    assert(processor.brokenListeners.isEmpty)
    assert(processor.isThereActiveListener)
  }

  it must "close the HB"  in new Fixture {
    processor.init()
    processor.close()
    assert(processor.hbEngine.isClosed)
  }

  it must "close the producer"  in new Fixture {
    processor.init()
    processor.close()
    assert(inputsProvider.isClosed)
  }

  it must "not process events before being inited" in new Fixture {
    val value: IASValue[?] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))
  }

  it must "process events after being inited" in new Fixture {
    processor.init()
    val value: IASValue[?] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(listeners.forall(_.callstoUserDefinedProcess.get()==1))
    processor.close()
  }

  it must "not process events after being closed" in new Fixture {
    processor.init()
    processor.close()
    val value: IASValue[?] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))
  }

  it must "discard unrecognized (not in CDB) values" in new Fixture {
    processor.init()
    val value: IASValue[?] = buildValue("Unrecognized id",Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(listeners.forall(_.callstoUserDefinedProcess.get()==0))
    processor.close()
  }

  it must "recognized templated values" in new Fixture {
    processor.init()

    val templatedId = Identifier.buildIdFromTemplate("TemplatedId",12)
    println("Submitting a value with ID "+templatedId)
    assert(Identifier.isTemplatedIdentifier(templatedId))
    val value: IASValue[?] = buildValue(templatedId,Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(listeners.forall(_.callstoUserDefinedProcess.get()==1))
    processor.close()
  }

  it must "process many events" in new Fixture {
    processor.init()

    val alarms = List(
      Alarm.getInitialAlarmState,
      Alarm.getInitialAlarmState(Priority.CRITICAL).set(),
      Alarm.getInitialAlarmState(Priority.HIGH).set(),
      Alarm.getInitialAlarmState(Priority.LOW).set(),
      Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    val valuesToSend = for {
      alarm <- alarms
      iasioDao <- iasiosDaos
      if (iasioDao.getIasType == IasTypeDao.ALARM)
    } yield  {
      buildValue(iasioDao.getId,alarm)
    }
    logger.info("Going to process {} IASIOs",valuesToSend.length)

    inputsProvider.sendInputs(valuesToSend.toSet)
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(listeners.forall(_.receivedValues.length==valuesToSend.length))
    processor.close()
  }

  it must "cope with excetion in the init" in new Fixture {
    processorWithFailures.init()

    assert(processorWithFailures.brokenListeners.length==1)
    assert(processorWithFailures.brokenListeners.map(_.id)==List(idOfFailingInit))
    assert(processorWithFailures.activeListeners.length==listenersWithFailure.length-1)
    assert(!processorWithFailures.activeListeners.map(_.id).contains(idOfFailingInit))
    processorWithFailures.close()
  }

  it must "cope with excetion in the process" in new Fixture {
    processorWithFailures.init()

    val alarms = List(
      Alarm.getInitialAlarmState,
      Alarm.getInitialAlarmState(Priority.CRITICAL).set(),
      Alarm.getInitialAlarmState(Priority.HIGH).set(),
      Alarm.getInitialAlarmState(Priority.LOW).set(),
      Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    val valuesToSend = for {
      alarm <- alarms
      iasioDao <- iasiosDaos
      if (iasioDao.getIasType == IasTypeDao.ALARM)
    } yield  {
      buildValue(iasioDao.getId,alarm)
    }
    logger.info("Going to process {} IASIOs with a failing processor",valuesToSend.length)

    inputsProviderWithFailures.sendInputs(valuesToSend.toSet)
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    // both the init and the process failed
    assert(processorWithFailures.brokenListeners.length==2)
    assert(processorWithFailures.brokenListeners.map(_.id)==List(idOfFailingInit, idOfFailinProcess))
    assert(processorWithFailures.activeListeners.length==listenersWithFailure.length-2)
    assert(!processorWithFailures.activeListeners.map(_.id).contains(idOfFailingInit))
    assert(!processorWithFailures.activeListeners.map(_.id).contains(idOfFailinProcess))

    val successufullListeners=listenersWithFailure.filter(l => l.id!=idOfFailinProcess && l.id!=idOfFailingInit)
    successufullListeners.foreach(l => assert(l.receivedValues.length==valuesToSend.length))
    val failingListeners=listenersWithFailure.filter(l => l.id==idOfFailinProcess && l.id==idOfFailingInit)
    failingListeners.foreach(l => assert(l.receivedValues.length==0))
    processorWithFailures.close()
  }

  it must "close not responding (timeout) threads" in new Fixture {
    processorWithTO.init()

    // Send one value to trigger the timeout
    val value: IASValue[?] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.getInitialAlarmState(Priority.MEDIUM).set())
    inputsProviderWithTO.sendInputs(Set(value))

    logger.info("Giving thread time to fail")
    Thread.sleep((timeout+10)*1000)
    logger.info("Timeout elapsed")

    // The listener with timeout must be marked as broken
    assert(processorWithTO.brokenListeners.length==1)
    assert(processorWithTO.brokenListeners.map(_.id).contains(idOfFailingTO))
    assert(processorWithTO.activeListeners.length==listenersWithTO.length-1)

    // Send another value
    inputsProviderWithTO.sendInputs(Set(value))

    listenersWithTO.forall(l => l.callstoUserDefinedInit.get()==2)
    listenersWithTO.filterNot(_.id==idOfFailingTO).forall(l => l.callstoUserDefinedProcess.get()==2)
    listenersWithTO.filter(_.id==idOfFailingTO).forall(l => l.callstoUserDefinedProcess.get()==1)
    listenersWithTO.forall(l => l.callstoUserDefinedClose.get()==2)
    processorWithTO.close()
  }
}
