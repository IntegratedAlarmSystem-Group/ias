package org.eso.ias.kafkaneo.test.consumer

import ch.qos.logback.classic.Level
import org.eso.ias.kafkaneo.IasTopic
import org.eso.ias.kafkaneo.consumer.{Consumer, ConsumerHelper, ConsumerListener}
import org.eso.ias.kafkautils.SimpleStringProducer
import org.eso.ias.logging.IASLogger
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap}
import org.scalatest.flatspec.AnyFlatSpec

class ConsumerTest extends AnyFlatSpec with BeforeAndAfterAllConfigMap {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  IASLogger.setRootLogLevel(Level.DEBUG)

  /** The producer of events */
  private val producer = new SimpleStringProducer(ConsumerHelper.DefaultKafkaBroker, "ConsumerTestProducer")

  override protected def beforeAll(configMap: ConfigMap): Unit = {
    println(s"\n\nCONFIG: ${configMap.keySet.mkString}\n\n")
    producer.setUp()
  }

  override protected def afterAll(configMap: ConfigMap): Unit = {
    producer.tearDown()
  }

  /**
   * Wait until the number of received items have been received or the timeout elapsed
   *
   * @param n The number of items to be received by the listener
   * @param ml THe listener of items
   * @param maxTimeout The max timeout (msec) to wait until all the items have been received
   * @return The number of received events
   */
  def waitForItems(n: Int, ml: MockListener, maxTimeout: Int): Int = {
    require(n>0)
    require(maxTimeout>0)

    val endTime = System.currentTimeMillis()+maxTimeout
    while (ml.size<n && System.currentTimeMillis()<endTime) { Thread.sleep(100) }
    ml.size
  }

  behavior of "The Kafka Neo Consumer"

  it should  "Add and remove listeners" in {
    logger.info("Testing listeners started")
    val consumer = new Consumer[String, String]("Test1Id", "Test1Group")
    assert(consumer.id=="Test1Id")
    assert(consumer.groupId=="Test1Group")

    val ml1 = new MockListener(IasTopic.Test)
    consumer.addListener(ml1)
    val ml2 = new MockListener(IasTopic.Test)
    consumer.addListener(ml2)
    val ml3 = new MockListener(IasTopic.Core)
    consumer.addListener(ml3)
    val ml4 = new MockListener(IasTopic.Heartbeat)
    consumer.addListener(ml4)

    val testListeners = consumer.getListeners(IasTopic.Test)
    assert(testListeners.size==2)
    val hbListeners = consumer.getListeners(IasTopic.Heartbeat)
    assert(hbListeners.size==1)
    val coreListeners = consumer.getListeners(IasTopic.Core)
    assert(coreListeners.size==1)

    assert(consumer.removeListener(ml2))
    val testListeners2 = consumer.getListeners(IasTopic.Test)
    assert(testListeners2.size==1)
    assert(!consumer.removeListener(ml2))
    assert(testListeners2.size==1)

    assert(consumer.removeListener(ml3))
    val coreListeners2 = consumer.getListeners(IasTopic.Core)
    assert(coreListeners2.isEmpty)

    val ml5 = new MockListener(IasTopic.Core)
    consumer.addListener(ml5)
    val ml6 = new MockListener(IasTopic.Core)
    consumer.addListener(ml6)
    consumer.addListener(ml3)
    val coreListeners3 = consumer.getListeners(IasTopic.Core)
    assert(coreListeners3.size==3)

    consumer.addListener(ml3) // Already present
    val coreListeners4 = consumer.getListeners(IasTopic.Core)
    assert(coreListeners4.size==3)

    logger.info("Testing listeners done")
  }

  it should "Initialize and close the consumer" in {
    logger.info("Testing initialization and closing")
    val consumer = new Consumer[String, String]("Test2Id", "Test2Group")
    val ml1 = new MockListener(IasTopic.Test)
    consumer.addListener(ml1)

    logger.info("Initializing the consumer")
    consumer.init()
    logger.info("Giving the consumer time to init")
    Thread.sleep(10000)
    logger.info("Closing the consumer")
    consumer.close()
    logger.info("Consumer closed. Test terminated")
  }

  it should "get events from one topic" in {
    logger.info("Testing the getting of events from one single topic")
    val consumer = new Consumer[String, String]("Test2Id", "Test2Group")
    val ml1 = new MockListener(IasTopic.Test)
    ml1.enable()
    consumer.addListener(ml1)

    logger.info("Initializing the consumer")
    consumer.init()
    logger.info("Giving the consumer time to init")
    Thread.sleep(10000)

    val msg = "DataMsg-"
    val k = "Key-"
    val itemsToPush = 10

    val itemToSend: Seq[(String, String)] = for {
      i <- 1 to itemsToPush
    } yield (s"$msg$i", s"$k$i")

    for (item <- itemToSend) {
      logger.info(s"Pushing v=${item._1},k=${item._2} in ${IasTopic.Test.kafkaTopicName}")
      producer.push(item._1, IasTopic.Test.kafkaTopicName, null, item._2)
    }
    producer.flush()

    logger.info("Waiting to receive the item")
    assert(waitForItems(itemsToPush, ml1, 10000)==10)

    val receivedItems = ml1.getRecvEvents().map(r =>  (r.value, r.key) )
    assert(itemToSend==receivedItems)

    logger.info("Closing the consumer")
    consumer.close()
    logger.info("Consumer closed. Test terminated")
  }

  it must "send the items to all the listeners" in {
    logger.info("Testing if the consumer sends the events to all the listeners of a topic")
    val consumer = new Consumer[String, String]("Test2Id", "Test2Group")
    val ml1 = new MockListener(IasTopic.Test)
    ml1.enable()
    consumer.addListener(ml1)
    val ml2 = new MockListener(IasTopic.Test)
    ml2.enable()
    consumer.addListener(ml2)
    val ml3 = new MockListener(IasTopic.Test)
    ml3.enable()
    consumer.addListener(ml3)

    // Check also if events are pushed to wrong listeners
    val ml4 = new MockListener(IasTopic.Command)
    ml4.enable()
    consumer.addListener(ml4)
    val ml5 = new MockListener(IasTopic.Core)
    ml5.enable()
    consumer.addListener(ml5)
    val ml6 = new MockListener(IasTopic.Heartbeat)
    ml6.enable()
    consumer.addListener(ml6)
    val ml7 = new MockListener(IasTopic.Plugins)
    ml7.enable()
    consumer.addListener(ml7)
    val ml8 = new MockListener(IasTopic.Reply)
    ml8.enable()
    consumer.addListener(ml8)

    logger.info("Initializing the consumer")
    consumer.init()
    logger.info("Giving the consumer time to init")
    Thread.sleep(10000)

    val msg = "DataMsg-"
    val k = "Key-"
    val itemsToPush = 10

    val itemToSend: Seq[(String, String)] = for {
      i <- 1 to itemsToPush
    } yield (s"$msg$i", s"$k$i")

    for (item <- itemToSend) {
      logger.info(s"Pushing v=${item._1},k=${item._2} in ${IasTopic.Test.kafkaTopicName}")
      producer.push(item._1, IasTopic.Test.kafkaTopicName, null, item._2)
    }
    producer.flush()

    logger.info("Waiting to receive the item")
    assert(waitForItems(itemsToPush, ml1, 10000)==itemsToPush)
    assert(waitForItems(itemsToPush, ml2, 10000)==itemsToPush)
    assert(waitForItems(itemsToPush, ml3, 10000)==itemsToPush)

    logger.info("Check if events are sent only to the listener of the right topic")
    assert(ml4.isEmpty, s"Unexpected items received in CMD topic ${ml4.getRecvEvents().mkString}")
    assert(ml5.isEmpty, s"Unexpected items received in Core topic ${ml5.getRecvEvents().mkString}")
    assert(ml6.isEmpty, s"Unexpected items received in Hb topic ${ml6.getRecvEvents().mkString}")
    assert(ml7.isEmpty, s"Unexpected items received in Plugins topic ${ml7.getRecvEvents().mkString}")
    assert(ml8.isEmpty, s"Unexpected items received in Reply topic ${ml8.getRecvEvents().mkString}")

    val receivedItems1 = ml1.getRecvEvents().map(r =>  (r.value, r.key) )
    assert(itemToSend==receivedItems1)
    val receivedItems2 = ml2.getRecvEvents().map(r =>  (r.value, r.key) )
    assert(itemToSend==receivedItems2)
    val receivedItems3 = ml3.getRecvEvents().map(r =>  (r.value, r.key) )
    assert(itemToSend==receivedItems3)

    logger.info("Closing the consumer")
    consumer.close()
    logger.info("Consumer closed. Test terminated")
  }

  it should "get and dispatch events from multiple topics" in {

    def builEventsForTopic(topic: IasTopic, nEvents: Int): List[(String, String, String)] = {
      val msg = s"$topic-DataMsg-"
      val k = s"$topic-Key-"

      val ret =for {
        i <- 1 to nEvents
      } yield (s"$msg$i", s"$k$i", topic.kafkaTopicName)
      ret.toList
    }

    logger.info("Testing the getting of events from several topics")

    val consumer = new Consumer[String, String]("Test5Id", "Test5Group")
    val ml1 = new MockListener(IasTopic.Test)
    ml1.enable()
    consumer.addListener(ml1)

    val ml2 = new MockListener(IasTopic.Core)
    ml2.enable()
    consumer.addListener(ml2)

    val ml3 = new MockListener(IasTopic.Heartbeat)
    ml3.enable()
    consumer.addListener(ml3)

    val ml4 = new MockListener(IasTopic.Plugins)
    ml4.enable()
    consumer.addListener(ml4)

    logger.info("Initializing the consumer")
    consumer.init()
    logger.info("Giving the consumer time to initialize")
    Thread.sleep(10000)


    val numOfTestEvents = 15
    val testEvents = builEventsForTopic(IasTopic.Test, numOfTestEvents)

    val numOfCoreEvents = 55
    val coreEvents = builEventsForTopic(IasTopic.Core, numOfCoreEvents)

    val numOfHbEvents = 35
    val hbEvents = builEventsForTopic(IasTopic.Heartbeat, numOfHbEvents)

    val numOfpluginsEvents = 46
    val pluginsEvents = builEventsForTopic(IasTopic.Plugins, numOfpluginsEvents)

    val numOfCmdEvents = 8
    val cmdEvents = builEventsForTopic(IasTopic.Command, numOfCmdEvents)

    val totEventsToSend: List[(String, String, String)] = testEvents:::coreEvents:::hbEvents:::pluginsEvents:::cmdEvents

    for (item <- totEventsToSend) {
      logger.info(s"Pushing v=${item._1},k=${item._2} in ${item._3}")
      producer.push(item._1, item._3, null, item._2)
    }
    producer.flush()

    logger.info("Waiting to receive the item from the topics")
    assert(waitForItems(numOfTestEvents, ml1, 10000)==numOfTestEvents, s"Wrong number of items from TEST topic ${ml1.getRecvEvents().mkString}")
    assert(waitForItems(numOfCoreEvents, ml2, 10000)==numOfCoreEvents, s"Wrong number of items from CORE topic ${ml2.getRecvEvents().mkString}")
    assert(waitForItems(numOfHbEvents, ml3, 10000)==numOfHbEvents, s"Wrong number of items from HB topic ${ml3.getRecvEvents().mkString}")
    assert(waitForItems(numOfpluginsEvents, ml4, 10000)==numOfpluginsEvents, s"Wrong number of items from PLUGIN topic ${ml4.getRecvEvents().mkString}")

    logger.info("Checking if events have been delivered to the right listeners")
    val rcvTestItems = ml1.getRecvEvents().map(r => (r.value, r.key))
    assert(testEvents.map( i => (i._1, i._2)) == rcvTestItems)
    val rcvCoreItems = ml2.getRecvEvents().map(r => (r.value, r.key))
    assert(coreEvents.map( i => (i._1, i._2)) == rcvCoreItems)
    val rcvHbItems = ml3.getRecvEvents().map(r => (r.value, r.key))
    assert(hbEvents .map( i => (i._1, i._2))== rcvHbItems)
    val rcvPluginItems = ml4.getRecvEvents().map(r => (r.value, r.key))
    assert(pluginsEvents.map( i => (i._1, i._2)) == rcvPluginItems)

    logger.info("Closing the consumer")
    consumer.close()
    logger.info("Consumer closed. Test terminated")
  }

}
