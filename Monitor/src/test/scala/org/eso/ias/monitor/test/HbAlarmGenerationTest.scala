package org.eso.ias.monitor.test

import java.util.concurrent.TimeUnit

import org.eso.ias.heartbeat.consumer.HbKafkaConsumer
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{Heartbeat, HeartbeatProducerType, HeartbeatStatus}
import org.eso.ias.kafkautils.{KafkaHelper, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.{HbMonitor, MonitorAlarm}
import org.eso.ias.types.Alarm
import scala.compiletime.uninitialized
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test the generation of alarms for missing HBs
  *
  * The test
  * - uses the config file in the test folder
  * - produces the HBs
  * - check the generation of alarms
  */
class HbAlarmGenerationTest extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach {

  /** The logger */
  val logger = IASLogger.getLogger(classOf[HbAlarmGenerationTest])

  /** The serializer of HBs */
  val hbSerializer = new HbJsonSerializer

  // HBs of the three plugins
  val p1Heartbeat = Heartbeat(HeartbeatProducerType.PLUGIN,"p1")
  val p2Heartbeat = Heartbeat(HeartbeatProducerType.PLUGIN,"p2")
  val p3Heartbeat = Heartbeat(HeartbeatProducerType.PLUGIN,"p3")
  val pluginHBs = Set(p1Heartbeat,p2Heartbeat,p3Heartbeat)
  val pluginIds = pluginHBs.map(_.name)

  // Identifiers of the 2 converters
  val conv1Heartbeat = Heartbeat(HeartbeatProducerType.CONVERTER,"conv1")
  val conv2Heartbeat = Heartbeat(HeartbeatProducerType.CONVERTER,"conv2")
  val converterHBs = Set(conv1Heartbeat, conv2Heartbeat)
  val converterIds = converterHBs.map(_.name)

  // Identifiers of the 3 clients
  val client1Heartbeat = Heartbeat(HeartbeatProducerType.CLIENT,"client1")
  val client2Heartbeat = Heartbeat(HeartbeatProducerType.CLIENT,"client2")
  val client3Heartbeat = Heartbeat(HeartbeatProducerType.CLIENT,"client3")
  val client4Heartbeat = Heartbeat(HeartbeatProducerType.CLIENT,"client4")
  val clientHBs = Set(client1Heartbeat,client2Heartbeat,client3Heartbeat,client4Heartbeat)
  val clientIds = clientHBs.map(_.name)

  // Identifier of the sink client
  val sink1Heartbeat = Heartbeat(HeartbeatProducerType.SINK,"sink1")
  val sinkHBs = Set(sink1Heartbeat)
  val sinkIds = sinkHBs.map(_.name)

  /** Identifiers of supervisors */
  val superv1Heartbeat = Heartbeat(HeartbeatProducerType.SUPERVISOR,"superv1")
  val superv12Heartbeat = Heartbeat(HeartbeatProducerType.SUPERVISOR,"superv2")
  val superv3Heartbeat = Heartbeat(HeartbeatProducerType.SUPERVISOR,"superv3")
  val supervisorHBs = Set(superv1Heartbeat,superv12Heartbeat,superv3Heartbeat)
  val supervisorIds = supervisorHBs.map(_.name)

  // Identifiers of the 2 converters
  val core1Heartbeat = Heartbeat(HeartbeatProducerType.CORETOOL,"ct1")
  val core2Heartbeat = Heartbeat(HeartbeatProducerType.CORETOOL,"ct2")
  val coreHBs = Set(core1Heartbeat, core2Heartbeat)
  val coreIds = coreHBs.map(_.name)

  /** The producer of HBs */
  var hbProducer: HbKafkaProducer = uninitialized

  /** The HB consumer to pass to the [[org.eso.ias.monitor.HbMonitor]] */
  var hbConsumer: HbKafkaConsumer = uninitialized

  /** The HB monitor to test */
  var hbMonitor: HbMonitor = uninitialized

  /** Threshold to send alarms if the HB has net been received */
  val threshold: Long = 5

  val id = "HBProducer-Test"

  /** The kafka string producer  */
  val simpleStringProd = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,id)

  /**
    * Sends the passed HBs
    *
    * @param heartbeats The HBs to send
    *
    */
  def sendHBs(heartbeats: Set[Heartbeat]): Unit = {
    heartbeats.foreach(heartbeat => {
      val str = hbSerializer.serializeToString(heartbeat,HeartbeatStatus.RUNNING,Map.empty, System.currentTimeMillis())
      hbProducer.push(str)
    })
  }

  override def beforeAll(): Unit = {

    simpleStringProd.setUp()
    hbProducer = new HbKafkaProducer(simpleStringProd,id,hbSerializer)
    hbProducer.init()

  }

  override def afterAll(): Unit = {
    hbProducer.shutdown()
    simpleStringProd.tearDown()
  }

  override def beforeEach() = {
    super.beforeEach()
    hbConsumer = new HbKafkaConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"HB-Consumer")

    hbMonitor = new HbMonitor(hbConsumer,pluginIds,converterIds,clientIds,sinkIds,supervisorIds,coreIds, threshold)
    hbMonitor.start()
  }

  override def afterEach(): Unit = {
    hbMonitor.shutdown()
    super.afterEach()
  }

  behavior of "The HB monitor"

  it must "properly get the IDs to monitor" in {
    assert(hbMonitor.pluginIds==pluginIds)
    assert(hbMonitor.converterIds==converterIds)
    assert(hbMonitor.clientIds==clientIds)
    assert(hbMonitor.sinkIds==sinkIds)
    assert(hbMonitor.supervisorIds==supervisorIds)
    assert(hbMonitor.coreToolIds==coreIds)
  }

  it must "properly get the threshold" in {
    assert(hbMonitor.threshold==threshold)
  }

  it must "set CLEAR all the alarms at the beginning" in {
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm==Alarm.getInitialAlarmState))
  }

  it must "SET all alarms if HB not received in time" in {
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm!=Alarm.getInitialAlarmState))
  }

  it must "CLEAR or SET the alarms when HB is/is not received" in {
    logger.info("Giving time to invalidate")
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm!=Alarm.getInitialAlarmState))
    sendHBs(pluginHBs)
    sendHBs(converterHBs)
    sendHBs(clientHBs)
    sendHBs(sinkHBs)
    sendHBs(supervisorHBs)
    sendHBs(coreHBs)
    logger.info("Giving time to update")
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => {
      logger.info("Alarm {} is {}",ma.name(),ma.getAlarm)
    })
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm.isCleared, ma.toString+" should be CLEAR"))

    logger.info("Giving time to invalidate")
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => {
      logger.info("State of {} is {}",ma.id,ma.getAlarm)
      assert(ma.getAlarm!=Alarm.getInitialAlarmState)}
    )
  }


}
