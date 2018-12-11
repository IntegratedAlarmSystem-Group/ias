package org.eso.ias.monitor.test

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

  // Identifiers of the 2 converters
  val conv1Identifier = new Identifier("conv1",IdentifierType.CONVERTER, Some(p1Identifier))
  val conv2Identifier = new Identifier("conv2",IdentifierType.CONVERTER, Some(p2Identifier))
  val converterIds = Set(conv1Identifier.id, conv2Identifier.id)

  // Identifiers of the 3 clients
  val client1Identifier = new Identifier("client1",IdentifierType.CLIENT,None)
  val client2Identifier = new Identifier("client2",IdentifierType.CLIENT,None)
  val client3Identifier = new Identifier("client3",IdentifierType.CLIENT,None)
  val client4Identifier = new Identifier("client4",IdentifierType.CLIENT,None)
  val clientIds = Set(client1Identifier.id,client2Identifier.id,client3Identifier.id,client4Identifier.id)

  // Identifier of the sink client
  val sink1Identifier = new Identifier("sink1",IdentifierType.SINK,None)
  val sinkIds = Set(sink1Identifier.id)

  /** Identifiers of supervisors */
  val superv1Identifier = new Identifier("superv1",IdentifierType.SUPERVISOR)
  val superv12dentifier = new Identifier("superv2",IdentifierType.SUPERVISOR)
  val superv3Identifier = new Identifier("superv3",IdentifierType.SUPERVISOR)
  val supervisorIds = Set(superv1Identifier.id,superv12dentifier.id,superv3Identifier.id)

  /** The producer of HBs */
  var hbProducer: HbKafkaProducer = _

  /** The HB consumer to pass to the [[org.eso.ias.monitor.HbMonitor]] */
  var hbConsumer: HbKafkaConsumer = _

  /** The HB monitor to test */
  var hbMonitor: HbMonitor = _

  /** Threshold to send alarms if the HB has net been received */
  val threshold: Long = 5

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
  }

  override def afterEach(): Unit = super.afterEach()

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

  it must "set all th ealarm CLEAR at the beginning" in {
    MonitorAlarm.values().foreach( ma => assert(ma.getAlarm==Alarm.CLEARED))
  }


}
