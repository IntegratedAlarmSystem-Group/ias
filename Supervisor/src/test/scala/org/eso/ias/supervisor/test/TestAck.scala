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
 */
class TestAck extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with IasioListener {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  val monSysId = new Identifier("MonitoredSystemID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
  val pluginId = new Identifier("SimulatedPluginID", IdentifierType.PLUGIN, monSysId)
  val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, pluginId)

  /** The ID of the temperature processed by a DASU of the Supervisor */
  val temperatureID = new Identifier("Temperature", IdentifierType.IASIO, converterId)

  /** The id of the alarm generated when the temperature goes over the thresholds */
  val temperatureAlarmId = Identifier("(SupervisorWithKafka:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(TemperatureAlarm:IASIO)")

  /** The ID of the strength processed by a DASU of the Supervisor */
  val strenghtID = new Identifier("Strenght", IdentifierType.IASIO, converterId)

  /** The ID of the supervisor */
  val supervisorId = "SupervisorWithKafka"

  val cmdSenderId = new Identifier("SupervAckTest", IdentifierType.CLIENT)

  /** The supervisor, external process */
  var supervisorProc: Process = _

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
  override def iasiosReceived(events: util.Collection[IASValue[_]]): Unit = {
    logger.info("{} alarms received", events.size())
    val iasios = CollectionConverters.asScala(events)
    iasios.filter(io => io.valueType==IASTypes.ALARM).foreach(e => {
      alarmsReceived.put(e.fullRunningId, e.asInstanceOf[IASValue[Alarm]])
      logger.info("Alarm {} stored ({} events in map)", e.fullRunningId, alarmsReceived.size())
    })
  }

  override def beforeAll(): Unit = {

    IASLogger.setRootLogLevel(Level.DEBUG)

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
  def buildIasioToSubmit(identifier: Identifier, value: Double): IASValue[_] = {
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

  it must "return ERROR if the output has not been generated yet" in {
    logger.info("Test started")
    val params = CollectionConverters.asJava(List("IASIO-ID", "User provided comment for ACK"))
    val props = CollectionConverters.asJava(Map[String, String]())
    val reply = cmdSender.sendSync(supervisorId, CommandType.ACK, params, props, 15, TimeUnit.SECONDS)
    assert(reply.isPresent)
    assert(reply.get().getExitStatus==CommandExitStatus.ERROR) // THE IASIO to ACK does not exist

    // Trigger the generation of the alarm
    val iasio = buildIasioToSubmit(temperatureID, 5)
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
    val iasio2 = buildIasioToSubmit(temperatureID, 100)
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
    val alToAck = "(SupervisorWithKafka:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(TemperatureAlarm:IASIO)"
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

    logger.info("Test terminated")
  }

}
