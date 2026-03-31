package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.consumer.{HbKafkaConsumer, HbListener, HbMsg}
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{Heartbeat, HeartbeatProducerType, HeartbeatStatus}
import org.eso.ias.kafkautils.{KafkaHelper, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.compiletime.uninitialized

/**
 * Test the HB kafka publisher and consumer
 */
class TestKafkaPublisher extends AnyFlatSpec with HbListener with BeforeAndAfterEach with BeforeAndAfterAll {
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[TestKafkaPublisher])
  
  /** The serializer */
  val serializer = new HbJsonSerializer
  
  /** The id of the HB producer */
  val id = "TheID"

  /** The ID of th esupervisor */
  val supervId = "SupervisorID"

  /** The HB of the supervisor */
  val supervHeartbeat = Heartbeat(HeartbeatProducerType.SUPERVISOR,supervId)

  /** The consumer receiving events from the HB topic */
  var hbKafkaConsumer: HbKafkaConsumer = uninitialized

  private val buffer: ListBuffer[HbMsg] = new ListBuffer[HbMsg]

  /** The stirng producer used by the  HbKafkaProducer */
  var stringProducer: SimpleStringProducer = uninitialized

  /** The kafka producer to test */
  var kProd: HbKafkaProducer = uninitialized

  def hbReceived(hbMsg: HbMsg): Unit = {
    require(Option(hbMsg).isDefined)
    buffer.append(hbMsg)
    logger.info("HB received: {}",hbMsg.toString)
  }

  override def beforeAll() = {
    stringProducer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"HbConsumer")
    stringProducer.setUp()
  }

  override def afterAll(): Unit = {
    stringProducer.tearDown()
  }

  override def beforeEach(): Unit = {
    logger.info("Initializing string consumer")
    buffer.clear()
    hbKafkaConsumer = new HbKafkaConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"HbConsumer")
    logger.info("Initializing HB kafka consumer")
    hbKafkaConsumer.addListener(this)
    hbKafkaConsumer.start()

    logger.info("Building HB kafka producer")
    kProd = new HbKafkaProducer(stringProducer, id, serializer)
    logger.info("Initializing HB kafka producer")
    kProd.init()
    
    logger.info("Initialized")
  }
  
  override def afterEach(): Unit = {
    logger.info("Shutting down the HB consumer")
    Option(hbKafkaConsumer).foreach(_.shutdown())

    logger.info("Shutting down HB kafka producer")
    Option(kProd).foreach(_.shutdown())
    
    logger.info("Shut down")
  }
  
  behavior of "The KafkaConsumer and the producer"
  
  they must "send correctly send and receive all the messages" in {
    
    // Sends one message for each HeartbeatStatus type
    val hbStates = HeartbeatStatus.values().foreach( state => {
      // One property just to have it
      val aProp = Map("PropK" -> state.toString())
      // Build the message
      val now = System.currentTimeMillis()
      
      kProd.send(supervHeartbeat,state,aProp)
      val op = Try(Thread.sleep(50))
      
    })
    
    // Give some time to read all strings
    val op = Try(Thread.sleep(1000))
    
    assert(buffer.size==HeartbeatStatus.values().length)
    
    assert(buffer.forall( msg => msg.hb.name==supervHeartbeat.name && msg.hb.hbType==supervHeartbeat.hbType))
    
    assert(HeartbeatStatus.values().forall( state => {
      buffer.filter(msg => msg.status==state).size==1
    }))
  }
  
}
