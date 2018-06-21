package org.eso.ias.sink.test

import java.nio.file.FileSystems
import java.util.Optional
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.{IasTypeDao, IasioDao}
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.{IasValueProcessor, ValueListener}
import org.eso.ias.types._
import org.scalatest.FlatSpec
import org.eso.ias.heartbeat.{HbMsgSerializer, HbProducer}
import org.eso.ias.types.IasValidity.UNRELIABLE

import scala.collection.JavaConverters._
import scala.collection.{JavaConverters, immutable}
import scala.collection.mutable.ListBuffer

// The following import is required by the usage of the fixture
import language.reflectiveCalls

/**
  * The listner for testing
  *
  * @param id The identifier to distinguish between many listeners int he same processor
  *           Mainly used for logging messages
  * @param throwExcInit if true the init throws an exception
  * @param throwExcProcess if true the process throws an exception
  */
class ListenerForTest(id: String,throwExcInit: Boolean=false, throwExcProcess: Boolean=false) extends ValueListener(id) {

  val callstoUserDefinedClose = new AtomicInteger(0)

  val callstoUserDefinedInit = new AtomicInteger(0)

  val callstoUserDefinedProcess = new AtomicInteger(0)

  /** The values received from the BSDB and to be processed */
  val receivedValues = ListBuffer[IASValue[_]]()

  override def close(): Unit = {
    callstoUserDefinedClose.incrementAndGet()
    logger.info("Listener {} closed",id)
  }

  override def init(): Unit = {
    callstoUserDefinedInit.incrementAndGet()
    logger.info("Listener {} initilized",id)
  }

  override def process(iasValues: List[IASValue[_]]): Unit = {
    callstoUserDefinedProcess.incrementAndGet()
    receivedValues.appendAll(iasValues)
    logger.info("Listener {} received {} values to process",id,iasValues.length)
  }
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

  /**
   * Push the string
   */
  override def push(hbAsString: String) {
    logger.info("HeartBeat [{}]",hbAsString)
    numOfHBs.incrementAndGet()
  }
}

/** Test the IasValueProcessor */
class ValueProcessorTest extends FlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(classOf[ValueProcessorTest])

  /** Fixture to build same type of objects for the tests */
  def fixture =
    new {
      /** The identifier */
      val processorIdentifier = new Identifier("ProcessorTestID", IdentifierType.SINK,None)

      // Build the listeners
      val listeners: immutable.IndexedSeq[ListenerForTest] = for {
        i <- 1 to 5
        id = s"ListenerID-$i"
        listener = new ListenerForTest(id)
      } yield listener

      val inputsProvider: DirectInputSubscriber = new DirectInputSubscriber()

      // Build the CDB reader
      val cdbParentPath = FileSystems.getDefault().getPath(".");
      val cdbFiles = new CdbJsonFiles(cdbParentPath)
      val cdbReader: CdbReader = new JsonReader(cdbFiles)

      val iasiosDaos: Set[IasioDao] = {
        val iasiosDaoJOpt = cdbReader.getIasios()
        assert(iasiosDaoJOpt.isPresent, "Error getting the IASIOs from the CDB")
        JavaConverters.asScalaSet(iasiosDaoJOpt.get()).toSet
      }

      /** The processor to test */
      val processor: IasValueProcessor  = new IasValueProcessor(
        processorIdentifier,
        listeners.toList,
        new HbProducerTest(new HbJsonSerializer()),
        inputsProvider,
        cdbReader)

    }

  /**
    * Build a IASValue to send to the processor
    *
    * @param id The identifier of the monitor point
    * @param d the value
    * @return the IASValue
    */
  def buildValue(id: String, d: Alarm): IASValue[_] = {

    // The identifier of the converter
    val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,None)

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
      t0+5,
      t0+10,
      t0+15,
      t0+20,
      null,
      null,
      null,
      null)
  }

  behavior of "The value processor"

  it must "not init and close at startup"  in {
    val f = fixture
    assert(!f.processor.closed.get())
    assert(!f.processor.initialized.get())
  }

  it must "init all the listeners"  in {
    val f = fixture
    f.processor.init()
    assert(!f.processor.closed.get())
    assert(f.processor.initialized.get())

    assert(f.processor.listeners.forall(_.initialized.get()))
    assert(f.processor.listeners.forall(!_.isBroken))

    assert(f.listeners.forall(_.callstoUserDefinedClose.get()==0))
    assert(f.listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))

    logger.info("Active listeners {}",f.processor.activeListeners().map(_.id).mkString(","))
    assert(f.processor.activeListeners().length==f.listeners.length)
    logger.info("broken listeners {}",f.processor.brokenListeners().map(_.id).mkString(","))
    assert(f.processor.brokenListeners().isEmpty)
    assert(f.processor.isThereActiveListener())
    f.processor.close()
  }

  it must "does not init the listeners more then once"  in {
    val f = fixture
    f.processor.init()
    f.processor.init()
    assert(!f.processor.closed.get())
    assert(f.processor.initialized.get())

    assert(f.processor.listeners.forall(_.initialized.get()))
    assert(f.processor.listeners.forall(!_.isBroken))

    assert(f.listeners.forall(_.callstoUserDefinedClose.get()==0))
    assert(f.listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))

    logger.info("Active listeners {}",f.processor.activeListeners().map(_.id).mkString(","))
    assert(f.processor.activeListeners().length==f.listeners.length)
    logger.info("broken listeners {}",f.processor.brokenListeners().map(_.id).mkString(","))
    assert(f.processor.brokenListeners().isEmpty)
    assert(f.processor.isThereActiveListener())
    f.processor.close()
  }
  it must "init the HB"  in {
    val f = fixture
    f.processor.init()
    assert(f.processor.hbEngine.isStarted)
    f.processor.close()
  }

  it must "init the producer"  in {
    val f = fixture
    f.processor.init()
    assert(f.inputsProvider.isInitialized)
    f.processor.close()
  }


  it must "close all the listeners"  in {
    val f = fixture
    f.processor.init()
    f.processor.close()
    assert(f.processor.closed.get())
    assert(f.processor.initialized.get())

    assert(f.processor.listeners.forall(_.initialized.get()))
    assert(f.processor.listeners.forall(!_.isBroken))
    assert(f.processor.listeners.forall(_.closed.get()))
    assert(f.processor.listeners.forall(!_.isBroken))

    assert(f.listeners.forall(_.callstoUserDefinedClose.get()==1))
    assert(f.listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))

    assert(f.processor.activeListeners().length==f.listeners.length)
    assert(f.processor.brokenListeners().isEmpty)
    assert(f.processor.isThereActiveListener())
  }

  it must "close all the listeners only once"  in {
    val f = fixture
    f.processor.init()
    f.processor.close()
    f.processor.close()
    assert(f.processor.closed.get())
    assert(f.processor.initialized.get())

    assert(f.processor.listeners.forall(_.initialized.get()))
    assert(f.processor.listeners.forall(!_.isBroken))
    assert(f.processor.listeners.forall(_.closed.get()))
    assert(f.processor.listeners.forall(!_.isBroken))

    assert(f.listeners.forall(_.callstoUserDefinedClose.get()==1))
    assert(f.listeners.forall(_.callstoUserDefinedInit.get()==1))
    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))

    assert(f.processor.activeListeners().length==f.listeners.length)
    assert(f.processor.brokenListeners().isEmpty)
    assert(f.processor.isThereActiveListener())
  }

  it must "close the HB"  in {
    val f = fixture
    f.processor.init()
    f.processor.close()
    assert(f.processor.hbEngine.isClosed)
  }

  it must "close the producer"  in {
    val f = fixture
    f.processor.init()
    f.processor.close()
    assert(f.inputsProvider.isClosed)
  }

  it must "not process events before being inited" in {
    val f = fixture
    val value: IASValue[_] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.SET_MEDIUM)
    f.inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))
  }

  it must "process events after being inited" in {
    val f = fixture
    f.processor.init()
    val value: IASValue[_] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.SET_MEDIUM)
    f.inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==1))
    f.processor.close()
  }

  it must "not process events after being closed" in {
    val f = fixture
    f.processor.init()
    f.processor.close()
    val value: IASValue[_] = buildValue("ASCEOnDasu2-ID1-Out",Alarm.SET_MEDIUM)
    f.inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))
  }

  it must "discard unrecognized (not in CDB) values" in {
    val f = fixture
    f.processor.init()
    val value: IASValue[_] = buildValue("Unrecognized id",Alarm.SET_MEDIUM)
    f.inputsProvider.sendInputs(Set(value))
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(f.listeners.forall(_.callstoUserDefinedProcess.get()==0))
    f.processor.close()
  }

  it must "process many events" in {
    val f = fixture
    f.processor.init()

    val alarms = List(Alarm.CLEARED, Alarm.SET_CRITICAL, Alarm.SET_HIGH, Alarm.SET_LOW, Alarm.SET_MEDIUM)
    val valuesToSend = for {
      alarm <- alarms
      iasioDao <- f.iasiosDaos
      if (iasioDao.getIasType == IasTypeDao.ALARM)
    } yield  {
      buildValue(iasioDao.getId,alarm)
    }
    logger.info("WGoing to process {} IASIOs",valuesToSend.length)

    f.inputsProvider.sendInputs(valuesToSend.toSet)
    Thread.sleep(2*IasValueProcessor.defaultPeriodicSendingTimeInterval)

    assert(f.listeners.forall(_.receivedValues.length==valuesToSend.length))
    f.processor.close()
  }

}
