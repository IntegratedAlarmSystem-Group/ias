package org.eso.ias.supervisor.inttest

import java.nio.file.{FileSystems, Paths}
import java.util.{Collection, Collections}
import java.util.concurrent.TimeUnit

import scala.jdk.CollectionConverters._
import scala.util.Try

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.subscriber.{InputSubscriber, KafkaSubscriber}
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosConsumer, KafkaIasiosProducer, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.types.*

/**
  * Integration test that runs a Supervisor with three DASUs
  * (two Python TFs via gRPC, one Scala TF) and verifies that
  * all three produce output on the Kafka BSDB.
  *
  * The PyTfServer is started as a subprocess before the Supervisor
  * is instantiated in-process. The Supervisor reads the PyTfServer
  * address from the CDB and connects to it via gRPC.
  * 
  * This integration tests verifies that Supervor runs scala and python TF at the same time
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

  /** Supervisor identifier. */
  private val supervisorId = new Identifier("supervisor", IdentifierType.SUPERVISOR, None)

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
  private var pyTfServerProc: Process = _

  /** The in-process Supervisor instance. */
  private var supervisor: Supervisor = _

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
      "SupervisorPyTfTest-Consumer"+System.currentTimeMillis(),
      null,
      null)
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

    logger.info("Instantiating Supervisor in-process")
    val cdbReader: CdbReader = new StructuredTextReader(
      FileSystems.getDefault.getPath(cdbPath).toFile)
    cdbReader.init()

    val inputConsumer: InputSubscriber = KafkaSubscriber(supervisorId.id, None, None, new java.util.Properties())

    val pyTfAddrOpt: Option[String] = if (pyTfServerAddr != null && !pyTfServerAddr.isBlank()) Some(pyTfServerAddr) else None

    val factory: (DasuDao, Identifier, OutputPublisher, InputSubscriber) => DasuImpl =
      (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) =>
        DasuImpl(dd, i, op, id, 1, 1, pyTfAddrOpt)

    supervisor = new Supervisor(
      supervisorId,
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      inputConsumer,
      cdbReader,
      factory,
      None)

    cdbReader.shutdown()

    assert(supervisor.start().isSuccess, "Supervisor failed to start")
    supervisor.enableAutoRefreshOfOutput(false)
    logger.info("Supervisor started successfully")

    alarmsReceived.clear()
    supervisor.enableAutoRefreshOfOutput(true)
  }

  override def afterEach(): Unit = {
    logger.info("Stopping Supervisor")
    if (supervisor != null) {
      supervisor.close()
      supervisor = null
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
        alarmsReceived.put(iasio.id, iasio.asInstanceOf[IASValue[Alarm]])
        logger.debug(s"Alarm ${iasio.id} added to the map (${alarmsReceived.size()} total)")
      } else {
        logger.debug(s"${iasio.id} not added to the map not being an alarm")
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

  /** Wait until all the `expected_alarm_ids` alarms are received or timeout elapses. */
  private def waitAlarms(expected_alarm_ids: List[String], timeoutSec: Int = 30): Boolean = {
    alarmsReceived.clear()
    logger.info(s"Waiting for ${expected_alarm_ids.length} alarms produced by all the DASUs: ${expected_alarm_ids.mkString(",")}...")
    val endTime = System.currentTimeMillis() + timeoutSec * 1000L

    while (System.currentTimeMillis() < endTime) {
      val recv_ids = alarmsReceived.keySet().asScala
      if (expected_alarm_ids.forall(recv_ids.contains)) return true
      logger.info(s"${recv_ids.size} alarms in buffer: ${recv_ids.mkString(",")}")
      Thread.sleep(250)
    }
    logger.warn(s"Only ${alarmsReceived.size()} alarms received, expected ${expected_alarm_ids.mkString(",")}")
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

    assert(waitAlarms(List("alarm_bool", "alarm_min_max", "alarm_multi")),
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
    iasiosProducer.push(buildAlarmIasio(al2MultiInputId, AlarmState.SET_UNACK))
    iasiosProducer.push(buildAlarmIasio(al3MultiInputId, AlarmState.SET_UNACK))

    assert(waitAlarms(List("alarm_bool", "alarm_min_max", "alarm_multi")), "Alarms should be SET initially")

    iasiosProducer.push(buildDoubleIasio(minMaxInputId, 1))
    iasiosProducer.push(buildBooleanIasio(boolInputId, false))
    iasiosProducer.push(buildAlarmIasio(al1MultiInputId, AlarmState.CLEAR_UNACK))
    iasiosProducer.push(buildAlarmIasio(al2MultiInputId, AlarmState.CLEAR_UNACK))
    iasiosProducer.push(buildAlarmIasio(al3MultiInputId, AlarmState.CLEAR_UNACK))

    logger.info("Waiting for cleared alarms...")
    assert(waitAlarms(List("alarm_bool", "alarm_min_max", "alarm_multi")), "Alarms should be SET initially")

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
