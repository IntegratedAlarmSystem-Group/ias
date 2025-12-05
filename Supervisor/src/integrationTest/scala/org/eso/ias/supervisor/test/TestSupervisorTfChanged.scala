package org.eso.ias.supervisor.test

import org.eso.ias.command.{CommandMessage, CommandSender, CommandType}
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosConsumer, KafkaIasiosProducer, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*
import org.scalactic.source.Position
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import java.io.{File, FileOutputStream}
import java.nio.file.Files
import java.util
import java.util.concurrent.TimeUnit
import java.util.{Collection, Collections}
import scala.jdk.javaapi.CollectionConverters
import scala.compiletime.uninitialized

/**
 * Test the functioning of the Supervisor when the TF changed and the TF_CHANGED command arrives.
 *
 * This test implicitly checks the restarting of the Supervisor.
 *
 * This test sends command to the Supervisor using the CommandSender, listens to IASIOs produced by the Supervisor
 * and sends inputs (IASIOs) to the Supervisor.
 *
 * Functioning of the test:
 * - starts the SupervisorToRestart supervisor java process
 * - listen to IASIOs produced by the Supervisor
 * - change the TF in the CDB without affecting the Supervisor and send TF_CHANGED command
 *           => the Supervisor must not restart
 * - change the TF in the CDB used by the Supervisor and send the TF_CHANGED command
 *          => the Supervisor must restart and use the new TF
 * - terminate the Supervisor with a SHUTDOWN command
 *
 * To check if the restarted Supervisor is using the new TF, this test sends a set of inputs and checks if the
 * output produced by the Supervisor matches with the algorithm of the used TF.
 */
class TestSupervisorTfChanged
  extends AnyFlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfter
    with IasioListener
{
  /** The ID of the Supervisor */
  val supervisorId = "SupervisorToRestart";

  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)

  /** The identifier of this IAS client */
  val commandSenderIdentifier = new Identifier("TestSupervRestartId",IdentifierType.CLIENT,None)

  /** The shared kafka string producer  */
  val stringProducer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,commandSenderIdentifier.id)

  /** The command sender to send commands to the Supervisor */
  var commandSender: CommandSender = uninitialized

  /** The producer to sends inputs to the Supervisor */
  var iasiosProducer: KafkaIasiosProducer = uninitialized

  /** The consumer to get the output of the Supervisor */
  var iasiosConsumer: KafkaIasiosConsumer = uninitialized

  /** The JSON serializer of IASIOs */
  val iasValueSerializer: IasValueStringSerializer = new IasValueJsonSerializer

  /** The IASIOs produced by the supervisor*/
  val iasiosReceived = Collections.synchronizedList(new util.Vector[IASValue[?]]())

  /** The identifier of the input of the supervisor */
  val inputIasioIdentifier = {
    val monSysId = new Identifier("MonitoredSystemID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
    val pluginId = new Identifier("SimulatedPluginID", IdentifierType.PLUGIN, monSysId)
    val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, pluginId)

    /** The ID of the temperature processed by a DASU of the Supervisor */
    new Identifier("InputOfSupervToRestart", IdentifierType.IASIO, converterId)
  }

  /** The path of the CDB */
  val cdbPath = "src/test"

  /**
   * Initialization of the tests:
   * - run the supervisor
   * - initialize the command sender
   */
  override def beforeAll(): Unit = {
    logger.info("Running the Supervisor {}",supervisorId)
    val cmdToRun: java.util.List[String] = new java.util.Vector[String]()
    cmdToRun.add("iasSupervisor")
    cmdToRun.add("SupervisorToRestart")
    cmdToRun.add("-j")
    cmdToRun.add(cdbPath)
    val procBuilder = new ProcessBuilder(cmdToRun)
    procBuilder.inheritIO()
    val p = procBuilder.start()

    // Initialize the command sender
    commandSender = new CommandSender(commandSenderIdentifier,stringProducer,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
    commandSender.setUp()

    // Initilaize the producer of IASIOs
    iasiosProducer = new KafkaIasiosProducer(
      stringProducer,
      KafkaHelper.IASIOs_TOPIC_NAME,
      iasValueSerializer)
    iasiosProducer.setUp()

    // Initialize the consumer of IASIOs
    val typeFilter = new util.HashSet[IASTypes]()
    typeFilter.add(IASTypes.ALARM)
    iasiosConsumer = new KafkaIasiosConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      commandSenderIdentifier.id,
      null,
      typeFilter
    )
    iasiosConsumer.setUp()
    iasiosConsumer.startGettingEvents(StreamPosition.END,this)

    // Give the supervisor time to start
    logger.info("Give the supervisor time to start...");
    Thread.sleep(10000);
    assert(p.isAlive());
    logger.info("Supervisor {} started",supervisorId);

    copyFile(s"$cdbPath/CDB/ASCE/ASCEOfSupervToRestart.orig",s"$cdbPath/CDB/ASCE/ASCEOfSupervToRestart.json")
  }

  override def afterAll(): Unit = {
    logger.debug("Shutting down the supervisor {}",supervisorId)
    // Shut the supervisor down
    commandSender.sendSync(supervisorId,CommandType.SHUTDOWN,null,null,30,TimeUnit.SECONDS)
    logger.debug("Closing the command sender")
    commandSender.close()
    logger.debug("Closing the producer of IASIOs")
    iasiosProducer.tearDown()
    logger.debug("Closing the consumer of IASIOs")
    iasiosConsumer.tearDown()

    copyFile(s"$cdbPath/CDB/ASCE/ASCEOfSupervToRestart.orig",s"$cdbPath/CDB/ASCE/ASCEOfSupervToRestart.json")

    logger.info("Closed")
  }

  override def before(fun: => Any)(implicit pos: Position): Unit = {
    super.before(fun)
    iasiosReceived.clear()
  }

  /** Invoked when a new IASIOs has been read from the topic */
  override def iasiosReceived(events: Collection[IASValue[?]]): Unit = {
    logger.debug("{} IASIOs received",events.size().toString)
    val iasios = CollectionConverters.asScala(events)
    iasios.foreach(iasio => {
      iasiosReceived.add(iasio)
      logger.debug("Received {}",iasio.toString)
    })
    logger.debug("{} IASIOS in the list",iasiosReceived.size().toString)
  }

  /**
   * Build a IASValue to send to the Supervisor
   */
  def buildIasioToSubmit(identifier: Identifier, value: Double): IASValue[?] = {
    val t0 = System.currentTimeMillis()-100
      IASValue.build(
        value,
        OperationalMode.OPERATIONAL,
        IasValidity.RELIABLE,
        identifier.fullRunningID,
        IASTypes.DOUBLE,
        t0,
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        t0+20,
        null, null,null)

  }

  /**
   * Change the TF of the ASCE the one in ASCEOfSupervToRestart.another
   */
  def changeAsceTF(): Unit = {
    logger.info("Changing ASCE TF (by changing the ASCE conf file)")
    val originalFile = new File("CDB/ASCE/ASCEOfSupervToRestart.json")
    val destFile = new File("CDB/ASCE/ASCEOfSupervToRestart.orig")
    originalFile.renameTo(destFile)

    val asceWithNewTfFile = new File("CDB/ASCE/ASCEOfSupervToRestart.another")
    val jsonFile = new File("CDB/ASCE/ASCEOfSupervToRestart.json")
    asceWithNewTfFile.renameTo(jsonFile)

  }

  /**
   * Copy files
   *
   * @param from Source file
   * @param to Destination file
   */
  def copyFile(from: String, to: String): Unit = {
    require(!Option(from).isEmpty && !from.isEmpty)
    require(!Option(to).isEmpty && !to.isEmpty)
    val destFfile = new File(to)
    if (destFfile.exists()) {
      destFfile.delete()
      logger.debug("File {} deleted",to)
    }

    val fromFile = new File(from)
    require(fromFile.exists(),to+" file does not exist!")
    val fromPath = fromFile.toPath

    val outStream = new FileOutputStream(destFfile)
    Files.copy(fromPath,outStream)

  }

  behavior of "The Supervisor"

  it must "restart and take a new TF" in {
    logger.debug("Test started")

    // Trigger an alarm
    val iasio = buildIasioToSubmit(inputIasioIdentifier,50D)
    iasiosProducer.push(iasio)
    Thread.sleep(10000)
    val lastIasio = iasiosReceived.get(iasiosReceived.size()-1)
    assert(lastIasio.value.asInstanceOf[Alarm]==Alarm.getInitialAlarmState.set())

    // Set a new TF: in this case the same min/max TF with different limits
    // is obtained by changing the ASCE configuration file
    logger.info("Changing the TF in the CDB")
    copyFile(s"$cdbPath/CDB/ASCE/ASCEOfSupervToRestart.another",s"$cdbPath/CDB/ASCE/ASCEOfSupervToRestart.json")

    logger.info("Sending TF_CHANGED command to the supervisors")
    val params: util.List[String] = new util.Vector[String]()
    params.add("org.eso.ias.asce.transfer.impls.MinMaxThresholdTF")
    commandSender.sendAsync(
      CommandMessage.BROADCAST_ADDRESS,
      CommandType.TF_CHANGED,
      params,
      null
    )

    // Give the supervisor time to restart
    logger.info("Giving the supervisor time to restart")
    Thread.sleep(20000)

    // Send another IASIO to check if the new TF has been taken up
    iasiosReceived.clear()
    val iasio2 = buildIasioToSubmit(inputIasioIdentifier,60D)

    // Send another IASIO so that the newly restarted supervisor begins to produce the output
    // If the new TF has been taken, there will be no alarm even if the value of iasio2 is greater than
    // that of iasio because the new TF has different thresholds
    iasiosProducer.push(iasio2)

    Thread.sleep(10000)
    val lastIasio2 = iasiosReceived.get(iasiosReceived.size()-1)
    assert(!lastIasio2.value.asInstanceOf[Alarm].isSet)
  }
}
