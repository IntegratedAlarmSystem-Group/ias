package org.eso.ias.heartbeat.test

import ch.qos.logback.classic.Level
import com.typesafe.scalalogging.Logger
import org.eso.ias.heartbeat.consumer.HbMsg
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.heartbeat.report.HbsCollector
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{HbProducer, Heartbeat, HeartbeatProducerType, HeartbeatStatus}
import org.eso.ias.kafkautils.{KafkaHelper, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.time.Duration
import scala.compiletime.uninitialized

/** Test the HbsCollector */
class TestHbsCollector extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(classOf[TestHbsCollector])

  /** The ID of the collector to test */
  val collectorID = "TestCollectorId"

  /** The serializer */
  val serializer = new HbJsonSerializer

  /** The TTL of HBs in the container */
  val ttl = 5000L

  /** The producer of HBs */
  var hbProducer: HbProducer = uninitialized

  /** The collector to test */
  var hbsCollector: HbsCollector = uninitialized

  /** The string producer used by the  HbKafkaProducer */
  var stringProducer: SimpleStringProducer = uninitialized

  override def beforeAll(): Unit = {
    IASLogger.setRootLogLevel(Level.DEBUG)
    logger.info("Setting up the SimpleStringProducer")
    stringProducer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"HbConsumer")
    stringProducer.setUp()
    logger.info("Setting up the HbKafkaProducer")
    hbProducer = new HbKafkaProducer(stringProducer, "TestCollectorProdID", serializer)
  }

  override def afterAll(): Unit = {
    stringProducer.tearDown()
  }

  override def beforeEach(): Unit = {
    logger.info("Building the HbsCollector to test")
    val id=s"${collectorID}_${System.currentTimeMillis()}"
    hbsCollector = HbsCollector(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      id,
      ttl)
    hbsCollector.setup()
    logger.info("Waiting until the HB collector is ready (connected to kafka)")
    val timeout = 10000
    val now = System.currentTimeMillis()
    while (!hbsCollector.isConsumerReady && System.currentTimeMillis()<now+timeout) {
      Thread.sleep(250)
    }
    if (!hbsCollector.isConsumerReady) {
      throw new Exception("Timeout: HB Consumer did not get ready in time")
    }
    logger.info(s"Collector with id=$id initialized and connected to the kafka topic")
  }

  override def afterEach(): Unit = {
    logger.info(s"Shutting down collector with id=${collectorID}")
    hbsCollector.shutdown()
  }

  /** Push an HB in the kafka topic */
  def pushHb(
              hbProdType: HeartbeatProducerType,
              id: String,
              status: HeartbeatStatus): Unit = {
    val hb = Heartbeat(hbProdType, id)
    hbProducer.send(hb, status, Map())
    logger.info(s"HB ${id}:${hbProdType} with status ${status} pushed in the BSDB")
  }

  /**
   * Wait until the collector contains the passed number of items or
   * the timeout elapses
   *
   * @param numOfItems The expected number of items in the collector
   * @return true if the items have been received or false in case of timeout
   *
   */
  def waitHbsReception(numOfItems: Int, timeout: Long): Boolean = {
    require(timeout>0)
    logger.info(s"Waiting to get ${numOfItems} items in the collector")
    val exitTime = System.currentTimeMillis()+timeout
    while (hbsCollector.size<numOfItems && System.currentTimeMillis()<exitTime) {
      Thread.sleep(250)
      logger.info(s"Items in collector: ${hbsCollector.size} (waiting until ${numOfItems} arrives)")
    }
    hbsCollector.size==numOfItems
  }

  /** Dump the content of the collector on the stdout */
  def dumpCollectorContent(): Unit = {
    val hbs: Seq[HbMsg] = hbsCollector.getHbs
    hbs.foreach(hbm => {
      println(s"HB ${hbm.hb.id} ${hbm.status}")
    })
  }

  behavior of "The HbsCollector container"

  it must "get an HB" in {
    logger.info("Check reception of one HB")
    hbsCollector.startCollectingHbs()
    pushHb(HeartbeatProducerType.PLUGIN, "TestId", HeartbeatStatus.STARTING_UP)
    // Give the container time to get the HB
    Thread.sleep(1000)
    val hbs = hbsCollector.getHbs
    assert(hbs.size==1)
  }

  it must "delete old HBs after TTL expires" in {
    logger.info("Check deletion of old HBs")
    hbsCollector.startCollectingHbs()
    pushHb(HeartbeatProducerType.PLUGIN, "TestId", HeartbeatStatus.STARTING_UP)
    pushHb(HeartbeatProducerType.SUPERVISOR, "TestId2", HeartbeatStatus.STARTING_UP)
    // Give the container time to get the HB
    Thread.sleep(1000)
    assert(hbsCollector.getHbs.size==2)
    logger.info("Waiting until TTL elapses")
    Thread.sleep(10000)
    assert(hbsCollector.isEmpty)
  }

  it must "not delete old HBs after TTL expires when paused" in {
    logger.info("Check pause")
    hbsCollector.startCollectingHbs()
    hbsCollector.pauseTtlCheck()
    pushHb(HeartbeatProducerType.PLUGIN, "TestId", HeartbeatStatus.STARTING_UP)
    pushHb(HeartbeatProducerType.SUPERVISOR, "TestId2", HeartbeatStatus.STARTING_UP)
    // Give the container time to get the HB
    Thread.sleep(1000);
    assert(hbsCollector.getHbs.size==2)
    logger.info("Waiting until TTL elapses")
    Thread.sleep(10000)
    assert(hbsCollector.getHbs.size==2)
  }

  it must "return the HBs of the requested tool type" in {
    logger.info("Check getting HBs of requested type")
    hbsCollector.startCollectingHbs()
    hbsCollector.pauseTtlCheck()
    assert(hbsCollector.isCollecting)
    pushHb(HeartbeatProducerType.PLUGIN, "TestId", HeartbeatStatus.STARTING_UP)
    pushHb(HeartbeatProducerType.SUPERVISOR, "TestId2", HeartbeatStatus.STARTING_UP)
    pushHb(HeartbeatProducerType.PLUGIN, "TestId3", HeartbeatStatus.RUNNING)
    pushHb(HeartbeatProducerType.CLIENT, "TestId4", HeartbeatStatus.PARTIALLY_RUNNING)
    pushHb(HeartbeatProducerType.CORETOOL, "TestId5", HeartbeatStatus.PAUSED)
    // Give the container time to get the HB
    Thread.sleep(1000)
    assert(hbsCollector.getHbs.size==5)
    val hbs = hbsCollector.getHbsOfType(HeartbeatProducerType.PLUGIN)
    hbs.foreach(hb => logger.info(s"Received Hb: ${hb.hb.id} ${hb.status}"))
    assert(hbs.size==2)
  }

  it must "return the HB of the requested tool" in {
    logger.info("Check getting HB of requested type/id")
    hbsCollector.startCollectingHbs()
    hbsCollector.pauseTtlCheck()
    assert(hbsCollector.isCollecting)

    pushHb(HeartbeatProducerType.PLUGIN, "TestId-H", HeartbeatStatus.EXITING)
    pushHb(HeartbeatProducerType.SUPERVISOR, "TestId2-H", HeartbeatStatus.STARTING_UP)
    pushHb(HeartbeatProducerType.PLUGIN, "TestId3-H", HeartbeatStatus.RUNNING)
    pushHb(HeartbeatProducerType.CLIENT, "TestId4-H", HeartbeatStatus.PARTIALLY_RUNNING)
    pushHb(HeartbeatProducerType.CORETOOL, "TestId5-H", HeartbeatStatus.PAUSED)
    // Wait until the HBs arrive

    val ret=waitHbsReception(5,15000)
    if (!ret) logger.error("Timeout waiting for items in the collector")
    assert(hbsCollector.getHbs.size==5)
    dumpCollectorContent()
    val hbOpt = hbsCollector.getHbOf(HeartbeatProducerType.SUPERVISOR,"TestId2-H")
    assert(hbOpt.nonEmpty)
    val hb = hbOpt.get
    assert(hb.status==HeartbeatStatus.STARTING_UP)
    assert(hb.hb.id=="TestId2-H:SUPERVISOR")
    assert(hb.hb.hbType==HeartbeatProducerType.SUPERVISOR)
  }

  it must "collect HBs for the poassed interval" in {
    logger.info("Check getting HBs along a time interval")
    hbsCollector.pauseTtlCheck()

    // HBs produced before the time interval shall be discarded
    hbsCollector.startCollectingHbs()
    pushHb(HeartbeatProducerType.PLUGIN, "ToBeDiscardedHb-1", HeartbeatStatus.EXITING)
    pushHb(HeartbeatProducerType.SUPERVISOR, "ToBeDiscardedHb-1", HeartbeatStatus.STARTING_UP)
    Thread.sleep(500)
    val d = Duration.ofSeconds(5)
    logger.info(s"Start collecting HB for ${d.getSeconds}")

    val t: Thread = new Thread(new Runnable {
      override def run(): Unit = {
        logger.info("Thread to collect HBs started")
        hbsCollector.collectHbsFor(d)
        logger.info("Thread to collect HBs terminated")
      }
    })
    t.start()
    Thread.sleep(500)
    pushHb(HeartbeatProducerType.PLUGIN, "TestId-H", HeartbeatStatus.EXITING)
    pushHb(HeartbeatProducerType.SUPERVISOR, "TestId2-H", HeartbeatStatus.STARTING_UP)
    pushHb(HeartbeatProducerType.PLUGIN, "TestId3-H", HeartbeatStatus.RUNNING)
    pushHb(HeartbeatProducerType.CLIENT, "TestId4-H", HeartbeatStatus.PARTIALLY_RUNNING)

    logger.info("Waiting for thread termination...")
    t.join(d.toMillis+1000)
    assert(!t.isAlive)
    assert(!hbsCollector.isCollecting)
    val hbs = hbsCollector.getHbs
    assert(hbs.size==4)
  }
}
