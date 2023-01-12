package org.eso.ias.dasu.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.{CdbFiles, CdbTxtFiles, StructuredTextReader, TextFileType}
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IasValidity.UNRELIABLE
import org.eso.ias.types.{IASTypes, IASValue, IasValueJsonSerializer, Identifier, IdentifierType, OperationalMode}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import ch.qos.logback.classic.Level

import java.nio.file.FileSystems
import scala.collection.mutable.ArrayBuffer

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
  var dasu: DasuImpl = _

  // The identifier of the monitored system
  val monSysId = new Identifier("ConverterID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)

  // The identifier of the plugin
  val pluginId = new Identifier("ConverterID", IdentifierType.PLUGIN, Some(monSysId))

  // The identifier of the converter
  val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, Some(pluginId))

  // The IDs of the monitor points in input
  val inputID1 = new Identifier("ACKASCE1-INPUT", IdentifierType.IASIO, converterId)
  val inputID2 = new Identifier("ACKASCE2-INPUT", IdentifierType.IASIO, converterId)

  IASLogger.setRootLogLevel(Level.DEBUG)

  /** Notifies about a new output produced by the DASU */
  override def outputEvent(output: IASValue[_]): Unit = {
    logger.info("Event received {}", output.id)
    outputValuesReceived.append(output)
  }

  /** Notifies about a new output produced by the DASU
   * formatted as String
   */
  override def outputStringifiedEvent(outputStr: String): Unit = {
    logger.info("String Event received: {}", outputStr)
  }

  val dasuDao: DasuDao = {
    cdbReader.init()
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }

    /** The output values published by the DASU */
  val outputValuesReceived = new ArrayBuffer[IASValue[_]]()

  def buildValue(mpId: Identifier, d: Long): IASValue[_] = {

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

  it must "Instantiate the DASU" in {
    logger.info("Test started")
    dasu.enableAutoRefreshOfOutput(false)

    val inputs: Set[IASValue[_]] = Set(buildValue(inputID1,0), buildValue(inputID2,0))
    inputsProvider.sendInputs(inputs)
    Thread.sleep(5000)
    logger.info("Test done")
  }

}
