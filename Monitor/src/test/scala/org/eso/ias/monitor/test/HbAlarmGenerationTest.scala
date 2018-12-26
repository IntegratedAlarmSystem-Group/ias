package org.eso.ias.monitor.test

import java.util.concurrent.TimeUnit

import org.eso.ias.heartbeat.HeartbeatStatus
import org.eso.ias.heartbeat.consumer.HbKafkaConsumer
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.{HbMonitor, MonitorAlarm}
import org.eso.ias.types.{Alarm, Identifier, IdentifierType}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec}

/**
  * Test the generation of alarms for missing HBs
  *
  * The test
  * - uses the config file in the test folder
  * - produces the HBs
  * - check the generation of alarms
  */
class HbAlarmGenerationTest extends FlatSpec with BeforeAndAfterAll with BeforeAndAfterEach {

  /** The logger */
  val logger = IASLogger.getLogger(classOf[HbAlarmGenerationTest])

  /** The identifir of the monitored system */
  val monitoredSystemIdentifier = new Identifier("MonSysId",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

  /** The serializer of HBs */
  val hbSerializer = new HbJsonSerializer

  // Identifier of the three plugins
  val p1Identifier = new Identifier("p1",IdentifierType.PLUGIN, Some(monitoredSystemIdentifier))
  val p2Identifier = new Identifier("p2",IdentifierType.PLUGIN, Some(monitoredSystemIdentifier))
  val p3Identifier = new Identifier("p3",IdentifierType.PLUGIN, Some(monitoredSystemIdentifier))

  val pluginIds = Set(p1Identifier.id,p2Identifier.id,p3Identifier.id)
  val pluginsFrIds = Set(p1Identifier.fullRunningID,p2Identifier.fullRunningID,p3Identifier.fullRunningID)

  // Identifiers of the 2 converters
  val conv1Identifier = new Identifier("conv1",IdentifierType.CONVERTER, Some(p1Identifier))
  val conv2Identifier = new Identifier("conv2",IdentifierType.CONVERTER, Some(p2Identifier))
  val converterIds = Set(conv1Identifier.id, conv2Identifier.id)
  val converterFrIds = Set(conv1Identifier.fullRunningID, conv2Identifier.fullRunningID)

  // Identifiers of the 3 clients
  val client1Identifier = new Identifier("client1",IdentifierType.CLIENT,None)
  val client2Identifier = new Identifier("client2",IdentifierType.CLIENT,None)
  val client3Identifier = new Identifier("client3",IdentifierType.CLIENT,None)
  val client4Identifier = new Identifier("client4",IdentifierType.CLIENT,None)
  val clientIds = Set(client1Identifier.id,client2Identifier.id,client3Identifier.id,client4Identifier.id)
  val clientFrIds = Set(
    client1Identifier.fullRunningID,
    client2Identifier.fullRunningID,
    client3Identifier.fullRunningID,
    client4Identifier.fullRunningID)

  // Identifier of the sink client
  val sink1Identifier = new Identifier("sink1",IdentifierType.SINK,None)
  val sinkIds = Set(sink1Identifier.id)
  val sinkFrIds = Set(sink1Identifier.fullRunningID)

  /** Identifiers of supervisors */
  val superv1Identifier = new Identifier("superv1",IdentifierType.SUPERVISOR)
  val superv12dentifier = new Identifier("superv2",IdentifierType.SUPERVISOR)
  val superv3Identifier = new Identifier("superv3",IdentifierType.SUPERVISOR)
  val supervisorIds = Set(superv1Identifier.id,superv12dentifier.id,superv3Identifier.id)
  val supervisorFrIds = Set(superv1Identifier.fullRunningID,superv12dentifier.fullRunningID,superv3Identifier.fullRunningID)

  /** The producer of HBs */
  var hbProducer: HbKafkaProducer = _

  /** The HB consumer to pass to the [[org.eso.ias.monitor.HbMonitor]] */
  var hbConsumer: HbKafkaConsumer = _

  /** The HB monitor to test */
  var hbMonitor: HbMonitor = _

  /** Threshold to send alarms if the HB has net been received */
  val threshold: Long = 5

  /**
    * Sends the HBs with the passed fullRunningIds
    * @param frIds The fullRunningIds of the HBs to send
    *
    */
  def sendHBs(frIds: Set[String]): Unit = {
    frIds.foreach(frid => {
      val str = hbSerializer.serializeToString(frid,HeartbeatStatus.RUNNING,Map.empty, System.currentTimeMillis())
      hbProducer.push(str)
    })
  }

  override def beforeAll(): Unit = {
    hbProducer = new HbKafkaProducer("HBProducer-Test",KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,hbSerializer)
    hbProducer.init()

  }

  override def afterAll(): Unit = {
    hbProducer.shutdown()
  }

  override def beforeEach() = {
    super.beforeEach()
    hbConsumer = new HbKafkaConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"HB-Consumer")

    hbMonitor = new HbMonitor(hbConsumer,pluginIds,converterIds,clientIds,sinkIds,supervisorIds,threshold)
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
  }

  it must "properly get the threshold" in {
    assert(hbMonitor.threshold==threshold)
  }

  it must "set CLEAR all the alarms at the beginning" in {
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm==Alarm.CLEARED))
  }

  it must "SET all alarms if HB not received in time" in {
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm!=Alarm.CLEARED))
  }

  it must "CLEAR or SET the alarms when HB is/is not received" in {
    logger.info("Giving time to invalidate")
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm!=Alarm.CLEARED))
    sendHBs(pluginsFrIds)
    sendHBs(converterIds)
    sendHBs(clientFrIds)
    sendHBs(sinkFrIds)
    sendHBs(supervisorFrIds)
    logger.info("Giving time to update")
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => {
      logger.info("Alarm {} is {}",ma.name(),ma.getAlarm)
    })
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm==Alarm.CLEARED,ma.toString+" should be CLEAR"))

    logger.info("Giving time to invalidate")
    Thread.sleep(TimeUnit.MILLISECONDS.convert(threshold+1,TimeUnit.SECONDS))
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm!=Alarm.CLEARED))
  }


}
