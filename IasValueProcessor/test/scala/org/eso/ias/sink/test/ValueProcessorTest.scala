package org.eso.ias.sink.test

import java.nio.file.FileSystems
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.{IasValueProcessor, ValueListener}
import org.eso.ias.types.{IASValue, IasValueJsonSerializer, Identifier, IdentifierType}
import org.scalatest.FlatSpec
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.heartbeat.{HbMsgSerializer, HbProducer}

import scala.collection.immutable
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

      val inputsProvider: InputSubscriber = new DirectInputSubscriber()

      // Build the CDB reader
      val cdbParentPath = FileSystems.getDefault().getPath(".");
      val cdbFiles = new CdbJsonFiles(cdbParentPath)
      val cdbReader: CdbReader = new JsonReader(cdbFiles)

      /** The processor to test */
      val processor: IasValueProcessor  = new IasValueProcessor(
        processorIdentifier,
        listeners.toList,
        new HbProducerTest(new HbJsonSerializer()),
        inputsProvider,
        cdbReader)

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
  }


}
