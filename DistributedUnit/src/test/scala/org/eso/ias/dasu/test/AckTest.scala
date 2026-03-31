package org.eso.ias.dasu.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.{CdbFiles, CdbTxtFiles, StructuredTextReader, TextFileType}
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IasValidity.UNRELIABLE
import org.eso.ias.types.{Alarm, IASTypes, IASValue, IasValueJsonSerializer, Identifier, IdentifierType, OperationalMode}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import ch.qos.logback.classic.Level

import java.nio.file.FileSystems
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, CountDownLatch, Executor, Executors, TimeUnit}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.compiletime.uninitialized

/**
 * Test the ACK of an alarm produced by a DASU.
 *
 * It uses a CDB with one DASU (ACKDASU) and three ASCEs:
 * - ASCE ACKASCE1: runs a MinMaxThreshold and get one input ACKASCE1-INPUT and output ACKASCE1-OUTPUT
 * - ASCE ACKASCE2: runs a MinMaxThreshold and get one input ACKASCE2-INPUT and output ACKASCE2-OUTPUT
 * - ASCE ACKASCE3: runs a Multiplicity TF and gets in input the alarms produced by ACKASCE1 and ACKASCE2
 *                  and produces the output ACKDASU3-Output
 */
class AckTest extends AnyFlatSpec with OutputListener with BeforeAndAfterEach {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);

  // Build the CDB reader
  val cdbParentPath = FileSystems.getDefault().getPath("src/test");
  val cdbFiles: CdbFiles = new CdbTxtFiles(cdbParentPath, TextFileType.JSON)
  val cdbReader: CdbReader = new StructuredTextReader(cdbFiles)
  cdbReader.init()

  val dasuId = "ACKDASU"

  val stringSerializer = Option(new IasValueJsonSerializer)
  val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(this, stringSerializer)

  val inputsProvider = new DirectInputSubscriber()

  // Build the Identifier
  val supervId = new Identifier("SupervId", IdentifierType.SUPERVISOR, None)
  val dasuIdentifier = new Identifier(dasuId, IdentifierType.DASU, supervId)

  // The DASU to test
  var dasu: DasuImpl = uninitialized
  // The identifier of the monitored system
  val monSysId = new Identifier("ConverterID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)

  // The identifier of the plugin
  val pluginId = new Identifier("ConverterID", IdentifierType.PLUGIN, Some(monSysId))

  // The identifier of the converter
  val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, Some(pluginId))

  // The IDs of the monitor points in input
  val inputID1 = new Identifier("ACKASCE1-INPUT", IdentifierType.IASIO, converterId)
  val inputID2 = new Identifier("ACKASCE2-INPUT", IdentifierType.IASIO, converterId)

  val dasuDao: DasuDao = {
    cdbReader.init()
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }

  /** The executor service for the Future that waits for events */
  val executorService: Executor = Executors.newCachedThreadPool()

  /** The output values published by the DASU */
  val outputValuesReceived: ArrayBlockingQueue[IASValue[?]] = new ArrayBlockingQueue[IASValue[?]](1000)

  IASLogger.setRootLogLevel(Level.DEBUG)

  /** Notifies about a new output produced by the DASU */
  override def outputEvent(output: IASValue[?]): Unit = {
    logger.info("Event received {}", output.id)
    outputValuesReceived.put(output)
  }

  /** Notifies about a new output produced by the DASU
   * formatted as String
   */
  override def outputStringifiedEvent(outputStr: String): Unit = {
    logger.info("String Event received: {}", outputStr)
  }

  /**
   * Wait until the specified number of events arrived or a timeout elapses.
   *
   * This shall be called through a Feature as it makes active polling over the list of received events
   *
   * @param numOfEvents the number of events to wait for before exiting
   * @param timeout the timeout (secs)
   * @return the events received
   */
  def waitEvents(numOfEvents: Int =1, acc: List[IASValue[?]]): List[IASValue[?]] = {
    require(numOfEvents>=0)
    logger.info("Waiting for {} events", numOfEvents)

    if (numOfEvents==0) acc.reverse
    else waitEvents(numOfEvents-1, outputValuesReceived.take()::acc)
  }

  def buildValue(mpId: Identifier, d: Long): IASValue[?] = {

    val t0 = System.currentTimeMillis() - 100

    IASValue.build(
      d,
      OperationalMode.OPERATIONAL,
      UNRELIABLE,
      mpId.fullRunningID,
      IASTypes.LONG,
      t0,
      t0 + 1,
      t0 + 5,
      t0 + 10,
      t0 + 15,
      null,
      null,
      null,
      null)
  }

  override def beforeEach(): Unit =  {
    outputValuesReceived.clear()
    logger.info("Building DASU {}", dasuIdentifier.id)
    dasu = new DasuImpl(dasuIdentifier, dasuDao, outputPublisher, inputsProvider, Integer.MAX_VALUE, Integer.MAX_VALUE)
    logger.info("Starting DASU {}", dasuIdentifier.id)
    dasu.start()
  }

  override def afterEach(): Unit = {
    logger.info("Closing DASU {}", dasuIdentifier.id)
    dasu.cleanUp()
  }

  behavior of "The ACK of the DASU"

  it must "ack an alarm" in {
    logger.info("Test of working ACK started")
    dasu.enableAutoRefreshOfOutput(false)

    val inputs: Set[IASValue[?]] = Set(buildValue(inputID1,0), buildValue(inputID2,0))

    // The Future to get the result
    val future: Future[List[IASValue[?]]] = Future {
      waitEvents(1, List())
    }(using ExecutionContext.fromExecutor(executorService))

    inputsProvider.sendInputs(inputs)

    val items: List[IASValue[?]] = Await.result(future, Duration(5, TimeUnit.SECONDS))

    val out = dasu.lastCalculatedOutput.get().get.value.asInstanceOf[Alarm]
    assert(out.isAcked)
    assert(out.isCleared)

    // Set the alarm
    val inputs2: Set[IASValue[?]] = Set(buildValue(inputID1,100), buildValue(inputID2,0))

    // The Future to get the result
    val future2: Future[List[IASValue[?]]] = Future {
      waitEvents(1, List())
    }(using ExecutionContext.fromExecutor(executorService))

    inputsProvider.sendInputs(inputs2)

    val items2: List[IASValue[?]] = Await.result(future2, Duration(5, TimeUnit.SECONDS))

    val out2 = dasu.lastCalculatedOutput.get().get.value.asInstanceOf[Alarm]
    assert(out2.isSet)
    assert(!out2.isAcked)

    // ACK
    // The Future to get the result
    val future3: Future[List[IASValue[?]]] = Future {
      waitEvents(1, List())
    }(using ExecutionContext.fromExecutor(executorService))

    dasu.ack(Identifier(dasu.lastCalculatedOutput.get().get.fullRunningId))

    val items3: List[IASValue[?]] = Await.result(future3, Duration(5, TimeUnit.SECONDS))
    logger.info("Received {} events", items.size)
    val out3 = items3.head.value.asInstanceOf[Alarm]
    assert(out3.isSet)
    assert(out3.isAcked)

    // Clear the alarm
    val inputs4: Set[IASValue[?]] = Set(buildValue(inputID1, 0), buildValue(inputID2, 0))

    // The Future to get the result
    val future4: Future[List[IASValue[?]]] = Future {
      waitEvents(1, List())
    }(using ExecutionContext.fromExecutor(executorService))

    inputsProvider.sendInputs(inputs4)
    val items4: List[IASValue[?]] = Await.result(future4, Duration(5, TimeUnit.SECONDS))
    val out4 = dasu.lastCalculatedOutput.get().get.value.asInstanceOf[Alarm]
    assert(out4.isCleared)
    assert(out4.isAcked)

    logger.info("Test done")
  }

  it must "return false when ACK is rejected" in {
    // Test the cases when the DASU rejects to ACK the passed ID
    logger.info("Test of rejected ACK")

    // Output not yet produced
    val id: Identifier = dasu.asceThatProducesTheOutput.output.id
    assert(!dasu.ack(id))

    // Try to ACK a value that is not an alarm neither is produced by the DASU
    assert(!dasu.ack(inputID1))

    // The ID of the alarm to ack has a wrong ASCE
    val wrongAsceFullId = Identifier("(SupervId:SUPERVISOR)@(ACKDASU:DASU)@(ACKASCE-WRONG:ASCE)@(ACKASCE3-OUTPUT:IASIO)")
    assert(!dasu.ack(wrongAsceFullId))

    // The ID of the alarm to ack has a wrong DASU
    val wrongDasuFullId = Identifier("(SupervId:SUPERVISOR)@(ACKDASU-WRONG:DASU)@(ACKASCE3:ASCE)@(ACKASCE3-OUTPUT:IASIO)")
    assert(!dasu.ack(wrongDasuFullId))

    // The ID of the alarm to ack has a wrong AISIO
    val wrongIasioFullId = Identifier("(SupervId:SUPERVISOR)@(ACKDASU:DASU)@(ACKASCE3:ASCE)@(ACKASCE3-WRONG:IASIO)")
    assert(!dasu.ack(wrongIasioFullId))

    logger.info("Test done")
  }

}
