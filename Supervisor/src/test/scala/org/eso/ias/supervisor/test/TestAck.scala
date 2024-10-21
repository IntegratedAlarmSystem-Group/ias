package org.eso.ias.supervisor.test

import ch.qos.logback.classic.Level
import org.eso.ias.command.{CommandExitStatus, CommandSender, CommandType}
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosConsumer, KafkaIasiosProducer, KafkaStringsConsumer, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes, IASValue, IasValidity, IasValueJsonSerializer, IasValueStringSerializer, Identifier, IdentifierType, OperationalMode}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec

import java.util
import java.util.Collections
import java.util.concurrent.{LinkedBlockingDeque, LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.javaapi.CollectionConverters
import scala.sys.process.*
import scala.compiletime.uninitialized

/**
 * Test the execution of the ACK command in the Supervisor.
 *
 * The test:
 * - run the SupervisorWithKafka supervisor as an external process
 * - set, clear and ack alarms produced by the DASUs of the supervisor by sending inputs and commands
 * - checks if the commands have been executed by checking the replies to the commands sent by the supervisor
 * - check if the IASIOS have been effectively set/cleared
 *
 * The ACK command is sent using the [[CommandSender]].
 *
 * The tests waits for a certain time (greater than the auto refresh time interval)
 * to be sure that the supervisor emits the alarms i.e. there is no other synchronization mechanism
 * than waiting for a resonable time before getting the alarms
 */
class TestAck extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with IasioListener {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  val monSysId = new Identifier("MonitoredSystemID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
  val pluginId = new Identifier("SimulatedPluginID", IdentifierType.PLUGIN, monSysId)
  val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, pluginId)

  /** The ID of the temperature processed by a DASU of the Supervisor */
  val temperatureId = new Identifier("Temperature", IdentifierType.IASIO, converterId)

  /** The id of the alarm generated when the temperature goes over the threshold */
  val temperatureAlarmId = Identifier("(SupervisorWithKafka:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(TemperatureAlarm:IASIO)")

  /** The id of the alarm generated when the strength goes over the threshold */
  val strengthAlarmId = Identifier("(SupervisorWithKafka:SUPERVISOR)@(DasuStrenght:DASU)@(AsceStrenght:ASCE)@(StrenghtAlarm:IASIO)")

  /** The ID of the strength processed by a DASU of the Supervisor */
  val strenghtId = new Identifier("Strenght", IdentifierType.IASIO, converterId)

  /** The ID of the supervisor */
  val supervisorId = "SupervisorWithKafka"

  val cmdSenderId = new Identifier("SupervAckTest", IdentifierType.CLIENT)

  /** The supervisor, external process */
  var supervisorProc: Process = uninitialized

  /** The shared kafka string producer */
  val  stringProducer: SimpleStringProducer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, cmdSenderId.fullRunningID)

  /** The command sender */
  val cmdSender: CommandSender = new CommandSender(cmdSenderId, stringProducer, KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)

  /** The produce to send IASIO so the supervisor */
  val iasiosProd: KafkaIasiosProducer = new KafkaIasiosProducer(
    new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, cmdSenderId.id),
    KafkaHelper.IASIOs_TOPIC_NAME,
    new IasValueJsonSerializer())

  val iasiosCons: KafkaIasiosConsumer = new KafkaIasiosConsumer (
    KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
    KafkaHelper.IASIOs_TOPIC_NAME,
    cmdSenderId.id,
    new util.HashSet[String](),
    CollectionConverters.asJava(Set(IASTypes.ALARM))) // The test processes only alarms

  /**
   * The test is only interested in the last received value of each alarm
   * Each alarm is stored in a map whose key is the fullRunningId of the alarm
   * and the value is the IASValue received from the listener
   */
  val alarmsReceived = Collections.synchronizedMap(new util.HashMap[String, IASValue[Alarm]]())

  /**
     * Process the IASIOs read from the kafka topic.
     *
     * @param events The IASIOs received in the topic
     */
  override def iasiosReceived(events: util.Collection[IASValue[?]]): Unit = {
    logger.info("{} alarms received", events.size())
    val iasios = CollectionConverters.asScala(events)
    iasios.filter(io => io.valueType==IASTypes.ALARM).foreach(e => {
      alarmsReceived.put(e.fullRunningId, e.asInstanceOf[IASValue[Alarm]])
      logger.info("Alarm {} stored ({} events in map)", e.fullRunningId, alarmsReceived.size())
    })
  }

  override def beforeAll(): Unit = {

//    IASLogger.setRootLogLevel(Level.DEBUG)

    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        println(s"\n\n\nException in thread ${t.getName}: ${e.getMessage}")
        println(e.printStackTrace())
      }
    })

    // Runs the supervisor
    logger.info("Starting the {} supervisor", supervisorId)
    val params = List("iasSupervisor", supervisorId, "-j", "src/test", "-x", "WARN")
    supervisorProc = Process(params).run()
    logger.info("Give the supervisor time to start and initialize")
    Thread.sleep(15000)
    assert(supervisorProc.isAlive())

    // Start the consumer of IASIOS
    iasiosCons.setUp()
    iasiosCons.startGettingEvents(KafkaStringsConsumer.StreamPosition.END, this)

    iasiosProd.setUp()

    cmdSender.setUp()
  }

  override def afterAll(): Unit = {

    logger.info("Closing the IASIO consumer")
    iasiosCons.tearDown()
    logger.info("Closing the IASIO producer")
    iasiosProd.tearDown()


    // Send a command to terminate the supervisor
    logger.info("Sending SHUTDOWN cmd to the supervisor")
    val reply = cmdSender.sendSync(supervisorId, CommandType.SHUTDOWN, null, null, 15, TimeUnit.SECONDS)
    if (!reply.isPresent) {
      logger.warn("No reply received by the Supervisor to the SHUTDOWN command")
    } else {
      if (reply.get().getExitStatus!=CommandExitStatus.OK) {
        logger.warn("The supervisor replied {} to the command to SHUTDOWN", reply.get().getExitStatus)
      }
    }
    cmdSender.close()
    Thread.sleep(10000)

    // If still alive, try to kill it
    if (supervisorProc.isAlive()) {
      logger.error("The supervisor accepted the command to SHUTDOWN but is still running!")
      logger.info("Try to forcibly kill the supervisor")
      supervisorProc.destroy()
    } else logger.info("The supervisor is dead")


  }

  override def beforeEach(): Unit = { alarmsReceived.clear() }

//  override def afterEach(): Unit = {}

  /** Build a [[IASValue]] to send to the supervisor  */
  def buildIasioToSubmit(identifier: Identifier, value: Double): IASValue[?] = {
    val t0 = System.currentTimeMillis() - 100
    IASValue.build(
      value,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      identifier.fullRunningID,
      IASTypes.DOUBLE,
      t0,
      t0 + 1,
      t0 + 5,
      t0 + 10,
      t0 + 15,
      t0 + 20,
      null, null, null)
  }

  behavior of "The ACK in the Supervisor"

  it must "ACK the alarm" in {
    logger.info("Test started")
    val params = CollectionConverters.asJava(List("IASIO-ID", "User provided comment for ACK"))
    val props = CollectionConverters.asJava(Map[String, String]())
    val reply = cmdSender.sendSync(supervisorId, CommandType.ACK, params, props, 15, TimeUnit.SECONDS)
    assert(reply.isPresent)
    assert(reply.get().getExitStatus==CommandExitStatus.ERROR) // THE IASIO to ACK does not exist

    // Trigger the generation of the alarm
    val iasio = buildIasioToSubmit(temperatureId, 5)
    iasiosProd.push(CollectionConverters.asJava(List(iasio)))
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    val alarmOpt = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    logger.info("Alarm1 retrieved from the queue")
    assert(alarmOpt.isDefined)
    logger.info("The alarm for the test is {}", alarmOpt.get.value.asInstanceOf[Alarm])
    assert(alarmOpt.get.value.isCleared)
    assert(alarmOpt.get.value.isAcked)

    // Set the alarm
    val iasio2 = buildIasioToSubmit(temperatureId, 100)
    iasiosProd.push(CollectionConverters.asJava(List(iasio2)))
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    val alarmOpt2 = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    logger.info("Alarm2 retrieved from the queue")
    assert(alarmOpt2.isDefined)
    logger.info("The alarm for the test is {}", alarmOpt2.get.value.asInstanceOf[Alarm])
    assert(alarmOpt2.get.value.isSet)
    assert(!alarmOpt2.get.value.isAcked)

    // ACK the alarm
    val alToAck = temperatureAlarmId.fullRunningID
    val params2 = CollectionConverters.asJava(List(alToAck, "User provided comment for ACK - correct"))
    logger.info("ACKing the alarm")
    val reply2 = cmdSender.sendSync(supervisorId, CommandType.ACK, params2, props, 15, TimeUnit.SECONDS)
    logger.info("ACK cmd sent")
    assert(reply2.isPresent)
    assert(reply2.get().getExitStatus==CommandExitStatus.OK) // The command to ACK has been executed
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    val alarmOpt3 = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    logger.info("Alarm3 retrieved from the queue")
    assert(alarmOpt3.isDefined)
    logger.info("The alarm for the test is {}", alarmOpt3.get.value)
    assert(alarmOpt3.get.value.isSet)
    assert(alarmOpt3.get.value.isAcked)

    // Clear the alarm (clean exit)
    val iasio3 = buildIasioToSubmit(temperatureId, 0)
    iasiosProd.push(CollectionConverters.asJava(List(iasio3)))

    logger.info("Test terminated")
  }

  it must "ack the right alarm" in {
    logger.info("Test for side effects started")
    // Check that if the supervisor has more active alarms, the ACK is forwarded
    // to the right DASU (i.e. no side effects)

    // Set bot the alarms produced by the Supervisor
    val temp =  buildIasioToSubmit(temperatureId, 100)
    val strength = buildIasioToSubmit(strenghtId, 100)
    iasiosProd.push(CollectionConverters.asJava(List(temp, strength)))
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    val tempAlOpt1 = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    assert(tempAlOpt1.isDefined)
    assert(tempAlOpt1.get.value.isSet)
    assert(!tempAlOpt1.get.value.isAcked)
    val strengthAlOpt1 = Option(alarmsReceived.get(strengthAlarmId.fullRunningID))
    assert(strengthAlOpt1.isDefined)
    assert(strengthAlOpt1.get.value.isSet)
    assert(!strengthAlOpt1.get.value.isAcked)

    // ACK strength alarm, temperature must remain un acked
    val alToAck = strengthAlarmId.fullRunningID
    val params = CollectionConverters.asJava(List(alToAck, "User provided comment for ACK - correct"))
    val props = CollectionConverters.asJava(Map[String, String]())
    logger.info("ACKing the strength alarm")
    val reply = cmdSender.sendSync(supervisorId, CommandType.ACK, params, props, 15, TimeUnit.SECONDS)
    logger.info("ACK cmd sent")
    assert(reply.isPresent)
    assert(reply.get().getExitStatus==CommandExitStatus.OK) // The command to ACK has been executed
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    // Check if only the strength alarm has been ACKed
    val tempAlOpt2 = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    assert(tempAlOpt2.isDefined)
    assert(tempAlOpt2.get.value.isSet)
    assert(!tempAlOpt2.get.value.isAcked)
    val strengthAlOpt2 = Option(alarmsReceived.get(strengthAlarmId.fullRunningID))
    assert(strengthAlOpt2.isDefined)
    assert(strengthAlOpt2.get.value.isSet)
    assert(strengthAlOpt2.get.value.isAcked)

    // Clear both alarms (one remains UN-ACKed)
    val temp2 =  buildIasioToSubmit(temperatureId, 0)
    val strength2 = buildIasioToSubmit(strenghtId, 0)
    iasiosProd.push(CollectionConverters.asJava(List(temp2, strength2)))
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    val tempAlOpt3 = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    assert(tempAlOpt3.isDefined)
    assert(tempAlOpt3.get.value.isCleared)
    assert(!tempAlOpt3.get.value.isAcked)
    val strengthAlOpt3 = Option(alarmsReceived.get(strengthAlarmId.fullRunningID))
    assert(strengthAlOpt3.isDefined)
    assert(strengthAlOpt3.get.value.isCleared)
    assert(strengthAlOpt3.get.value.isAcked)

    // Finally ACK the temperature alarm that is still UN-ACKed
    val tempAlarmToAck = temperatureAlarmId.fullRunningID
    val params2 = CollectionConverters.asJava(List(tempAlarmToAck, "User provided comment for ACK - correct"))
    logger.info("ACKing the temperature alarm asynchronously")
    cmdSender.sendAsync(supervisorId, CommandType.ACK, params2, props)
    logger.info("ACK cmd sent")
    logger.info("Giving time to get the updated alarm")
    Thread.sleep(10000) // The alarm is published continuously due to the refresh
    logger.info("Test resumed")
    // Check that the alarm has been ACKed
    val tempAlOpt4 = Option(alarmsReceived.get(temperatureAlarmId.fullRunningID))
    assert(tempAlOpt4.isDefined)
    assert(tempAlOpt4.get.value.isCleared)
    assert(tempAlOpt4.get.value.isAcked)
    val strengthAlOpt4 = Option(alarmsReceived.get(strengthAlarmId.fullRunningID))
    assert(strengthAlOpt4.isDefined)
    assert(strengthAlOpt4.get.value.isCleared)
    assert(strengthAlOpt4.get.value.isAcked)

    logger.info("Test for side effects done")
  }

}
