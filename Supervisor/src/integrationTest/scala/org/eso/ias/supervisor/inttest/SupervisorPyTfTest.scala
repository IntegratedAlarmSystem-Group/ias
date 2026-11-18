package org.eso.ias.supervisor.inttest

import java.nio.file.{FileSystems, Paths}
import java.util.{Collection, Collections}
import java.util.concurrent.TimeUnit

import scala.jdk.CollectionConverters._
import scala.util.Try

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosConsumer, KafkaIasiosProducer, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*

/**
  * Integration test that starts a Supervisor with three DASUs
  * (two Python TFs via gRPC, one Scala TF) and verifies that
  * all three produce output on the Kafka BSDB.
  *
  * The PyTfServer address is read from the CDB and the test starts
  * the PyTfServer subprocess on that port before each test.
  *
  * The Supervisor is started as an external process via `iasSupervisor`
  * using the CDB from `src/integrationTest/ias-pytf-cdb/`.
  */
class SupervisorPyTfTest
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with IasioListener {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  /** Input IASIO identifiers. */
  private val monSysId    = new Identifier("MonitoredSystemID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
  private val pluginId    = new Identifier("SimulatedPluginID", IdentifierType.PLUGIN, monSysId)
  private val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, pluginId)

  private val minMaxInputId   = new Identifier("min_max_input_val", IdentifierType.IASIO, converterId)
  private val boolInputId     = new Identifier("bool_input_val",  IdentifierType.IASIO, converterId)
  private val al1MultiInputId = new Identifier("al1_multi_input", IdentifierType.IASIO, converterId)
  private val al2MultiInputId = new Identifier("al2_multi_input", IdentifierType.IASIO, converterId)
  private val al3MultiInputId = new Identifier("al3_multi_input", IdentifierType.IASIO, converterId)

  /** CDB path. */
  private val cdbPath = "src/integrationTest/ias-pytf-cdb"

  /** PyTfServer address read from the CDB. */
  private var pyTfServerAddr: String = _

  /** Kafka producer to send inputs to the Supervisor. */
  private var iasiosProducer: KafkaIasiosProducer = _

  /** Kafka consumer that listens for alarm outputs. */
  private var iasiosConsumer: KafkaIasiosConsumer = _

  /** Collected alarm IASValues keyed by fullRunningId. */
  private val alarmsReceived = Collections.synchronizedMap(
    new java.util.HashMap[String, IASValue[Alarm]]())

  /** PyTfServer process handle. */
  private var pyTfServerProc: Process = null

  /** Supervisor process handle. */
  private var supervisorProc: Process = null

  override def beforeAll(): Unit = {
    logger.info("Reading pyTfServerAddr from CDB")
    val cdbReader: CdbReader = new StructuredTextReader(
      FileSystems.getDefault.getPath(cdbPath).toFile)
    cdbReader.init()
    val supervOpt = cdbReader.getSupervisor("supervisor")
    assert(supervOpt.isPresent, "Supervisor not found in CDB")
    pyTfServerAddr = supervOpt.get.getPyTfServerAddr()
    cdbReader.shutdown()
    logger.info(s"PyTfServer address from CDB: $pyTfServerAddr")

    val addrParts = pyTfServerAddr.split(":")
    assert(addrParts.length == 2, s"Invalid pyTfServerAddr: $pyTfServerAddr")

    logger.info("Setting up Kafka producer and consumer")

    val stringProducer = new SimpleStringProducer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, "SupervisorPyTfTest-Producer")

    iasiosProducer = new KafkaIasiosProducer(
      stringProducer,
      KafkaHelper.IASIOs_TOPIC_NAME,
      new IasValueJsonSerializer())
    iasiosProducer.setUp()

    val typeFilter = new java.util.HashSet[IASTypes]()
    typeFilter.add(IASTypes.ALARM)

    iasiosConsumer = new KafkaIasiosConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      "SupervisorPyTfTest-Consumer",
      null,
      typeFilter)
    iasiosConsumer.setUp()
    iasiosConsumer.startGettingEvents(StreamPosition.END, this)

    logger.info("Kafka producer and consumer ready")
  }

  override def beforeEach(): Unit = {
    val (host, port) = parseAddress(pyTfServerAddr)
    logger.info(s"Starting PyTfServer on $pyTfServerAddr")
    val pb = new ProcessBuilder("iasPyTfServer", "-p", port)
    pyTfServerProc = pb.start()
    Thread.sleep(2000)
    val pyAlive = Try(pyTfServerProc.exitValue()).isFailure
    assert(pyAlive || pyTfServerProc.isAlive(), "PyTfServer failed to start")
    logger.info(s"PyTfServer started on $pyTfServerAddr")

    logger.info(s"Starting Supervisor with CDB at $cdbPath")
    val spb = new ProcessBuilder("iasSupervisor", "supervisor", "-j", cdbPath, "-x", "WARN")
    supervisorProc = spb.start()
    logger.info("Giving the Supervisor time to start")
    Thread.sleep(20000)
    assert(supervisorProc.isAlive(), "Supervisor failed to start")
    logger.info("Supervisor started successfully")

    alarmsReceived.clear()
  }

  override def afterEach(): Unit = {
    logger.info("Stopping Supervisor")
    if (supervisorProc != null) {
      supervisorProc.destroy()
      Try(supervisorProc.waitFor(10, TimeUnit.SECONDS))
      supervisorProc = null
      logger.info("Supervisor stopped")
    }

    logger.info("Stopping PyTfServer")
    if (pyTfServerProc != null) {
      pyTfServerProc.destroy()
      Try(pyTfServerProc.waitFor(5, TimeUnit.SECONDS))
      pyTfServerProc = null
      logger.info("PyTfServer stopped")
    }
  }

  override def afterAll(): Unit = {
    logger.info("Tearing down Kafka components")
    iasiosProducer.tearDown()
    iasiosConsumer.tearDown()
    logger.info("All done")
  }

  /** Extract (host, port) from "host:port". */
  private def parseAddress(addr: String): (String, String) = {
    val parts = addr.split(":")
    (parts(0), parts(1))
  }

  /** Process alarm IASIOs received from Kafka. */
  override def iasiosReceived(events: Collection[IASValue[?]]): Unit = {
    val iasios = events.asScala
    iasios.foreach { iasio =>
      if (iasio.valueType == IASTypes.ALARM) {
        alarmsReceived.put(iasio.fullRunningId, iasio.asInstanceOf[IASValue[Alarm]])
        logger.debug(s"Alarm ${iasio.id} stored ({} total)", alarmsReceived.size())
      }
    }
  }

  /** Build a DOUBLE IASValue for submission to Kafka. */
  private def buildDoubleIasio(identifier: Identifier, value: Double): IASValue[_] = {
    val t0 = System.currentTimeMillis() - 100
    IASValue.build(
      value,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      identifier.fullRunningID,
      IASTypes.DOUBLE,
      t0, t0 + 1, t0 + 5, t0 + 10, t0 + 15, t0 + 20, null, null, null)
  }

  /** Build a BOOLEAN IASValue for submission to Kafka. */
  private def buildBooleanIasio(identifier: Identifier, value: Boolean): IASValue[_] = {
    val t0 = System.currentTimeMillis() - 100
    IASValue.build(
      value,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      identifier.fullRunningID,
      IASTypes.BOOLEAN,
      t0, t0 + 1, t0 + 5, t0 + 10, t0 + 15, t0 + 20, null, null, null)
  }

  /** Build an ALARM IASValue for submission to Kafka. */
  private def buildAlarmIasio(identifier: Identifier, state: AlarmState): IASValue[_] = {
    val t0 = System.currentTimeMillis() - 100
    val alarm = state match {
      case AlarmState.SET_UNACK | AlarmState.SET_ACK    => Alarm.getInitialAlarmState().set()
      case _                                            => Alarm.getInitialAlarmState()
    }
    IASValue.build(
      alarm,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      identifier.fullRunningID,
      IASTypes.ALARM,
      t0, t0 + 1, t0 + 5, t0 + 10, t0 + 15, t0 + 20, null, null, null)
  }

  /** Wait until at least `expected` alarms are received or timeout elapses. */
  private def waitAlarms(expected: Int, timeoutSec: Int = 30): Boolean = {
    val endTime = System.currentTimeMillis() + timeoutSec * 1000L
    while (System.currentTimeMillis() < endTime) {
      if (alarmsReceived.size() >= expected) return true
      Thread.sleep(500)
    }
    logger.warn(s"Only ${alarmsReceived.size()} alarms received, expected $expected")
    false
  }

  behavior of "The Supervisor with Python and Scala TFs"

  it must "produce outputs from all three DASUs when inputs are sent" in {
    logger.info("Sending inputs to all three DASUs")

    iasiosProducer.push(buildDoubleIasio(minMaxInputId, 50))
    iasiosProducer.push(buildBooleanIasio(boolInputId, true))
    iasiosProducer.push(buildAlarmIasio(al1MultiInputId, AlarmState.SET_UNACK))
    iasiosProducer.push(buildAlarmIasio(al2MultiInputId, AlarmState.SET_UNACK))
    iasiosProducer.push(buildAlarmIasio(al3MultiInputId, AlarmState.SET_UNACK))

    logger.info("Waiting for alarm outputs from all DASUs...")
    assert(waitAlarms(3, 30),
      s"Expected 3 alarms but received ${alarmsReceived.size()}: ${alarmsReceived.keySet()}")

    val minMaxOpt = alarmsReceived.keySet().asScala
      .find(_.toLowerCase.contains("alarm_min_max"))
    assert(minMaxOpt.isDefined, "MinMax alarm not found")

    val boolOpt = alarmsReceived.keySet().asScala
      .find(_.toLowerCase.contains("alarm_bool"))
    assert(boolOpt.isDefined, "BoolToAlarm alarm not found")

    val multiOpt = alarmsReceived.keySet().asScala
      .find(_.toLowerCase.contains("alarm_multi"))
    assert(multiOpt.isDefined, "Multiplicity alarm not found")

    assert(alarmsReceived.get(minMaxOpt.get).value.isSet,
      "MinMax alarm should be SET for value 50 > threshold 20")
    assert(alarmsReceived.get(multiOpt.get).value.isSet,
      "Multiplicity alarm should be SET when 3 inputs are SET")
    assert(alarmsReceived.get(boolOpt.get).value.isSet,
      "BoolToAlarm alarm should be SET when input is true")

    logger.info("All three outputs verified successfully")
  }

  it must "clear alarms when inputs go below threshold" in {
    logger.info("Sending inputs to set alarms, then clear them")

    iasiosProducer.push(buildDoubleIasio(minMaxInputId, 50))
    iasiosProducer.push(buildBooleanIasio(boolInputId, true))
    iasiosProducer.push(buildAlarmIasio(al1MultiInputId, AlarmState.SET_UNACK))
    Thread.sleep(10000)

    assert(waitAlarms(3), "Alarms should be SET initially")

    iasiosProducer.push(buildDoubleIasio(minMaxInputId, 1))
    iasiosProducer.push(buildBooleanIasio(boolInputId, false))
    iasiosProducer.push(buildAlarmIasio(al1MultiInputId, AlarmState.CLEAR_UNACK))
    iasiosProducer.push(buildAlarmIasio(al2MultiInputId, AlarmState.CLEAR_UNACK))
    iasiosProducer.push(buildAlarmIasio(al3MultiInputId, AlarmState.CLEAR_UNACK))

    logger.info("Waiting for cleared alarms...")
    Thread.sleep(10000)

    val minMaxOpt = alarmsReceived.keySet().asScala
      .find(_.toLowerCase.contains("alarm_min_max"))
    val boolOpt = alarmsReceived.keySet().asScala
      .find(_.toLowerCase.contains("alarm_bool"))
    val multiOpt = alarmsReceived.keySet().asScala
      .find(_.toLowerCase.contains("alarm_multi"))

    assert(minMaxOpt.isDefined, "MinMax alarm not found")
    assert(alarmsReceived.get(minMaxOpt.get).value.isCleared,
      "MinMax alarm should be CLEAR for value 1 < threshold 20")

    assert(boolOpt.isDefined, "BoolToAlarm alarm not found")
    assert(alarmsReceived.get(boolOpt.get).value.isCleared,
      "BoolToAlarm alarm should be CLEAR when input is false")

    assert(multiOpt.isDefined, "Multiplicity alarm not found")
    assert(alarmsReceived.get(multiOpt.get).value.isCleared,
      "Multiplicity alarm should be CLEAR when 0 inputs are SET")

    logger.info("All three alarms cleared successfully")
  }
}
