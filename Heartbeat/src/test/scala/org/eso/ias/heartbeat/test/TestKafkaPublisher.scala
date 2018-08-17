package org.eso.ias.heartbeat.test

import org.scalatest.FlatSpec
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.kafkautils.SimpleStringConsumer
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import scala.collection.mutable.ArrayBuffer
import org.scalatest.BeforeAndAfter
import org.eso.ias.heartbeat.HeartbeatStatus
import java.util.EnumSet
import scala.util.Try
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType

/**
 * Test the HB kafka publisher
 */
class TestKafkaPublisher extends FlatSpec with KafkaConsumerListener with BeforeAndAfter {
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[TestKafkaPublisher])
  
  /** The serializer */
  val serializer = new HbJsonSerializer
  
  /** The ide of the HB producer */
  val id = "TheID"
  
  /** The identifier of the supervisor */
  val supervIdentifier = new Identifier("SupervisorID", IdentifierType.SUPERVISOR, None)
  
  /**
   * The consumer getting events from the HB topic
   */
  val stringConsumer = new SimpleStringConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
      KafkaHelper.HEARTBEAT_TOPIC_NAME,
      "Tester-ID")
  
  /**
   * The strings read from the kafka topic
   */
  val receivedStrings: ArrayBuffer[String] = new ArrayBuffer
  
  /** The kafka producer to test */
  val kProd = new HbKafkaProducer(
      id,
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      serializer)
  
  override def stringEventReceived(event: String) {
    val str = Option(event)
    str.filter(!_.isEmpty).foreach(s => {
      logger.info("HB received {}",s)
      receivedStrings.append(s)
    })
  }
  
  before {
    logger.info("Initializing string consumer")
    receivedStrings.clear()
    stringConsumer.setUp()
    stringConsumer.startGettingEvents(StartPosition.END, this)
    
    logger.info("Initializing HB kafka producer")
    kProd.init()
    
    logger.info("Initialized")
  }
  
  after {
    logger.info("Shutting down HB kafka producer")
    kProd.shutdown()
    logger.info("Shutting down string consumer")
    stringConsumer.tearDown;
    
    logger.info("Shutted down")
  }
  
  behavior of "The KafkaConsumer" 
  
  it must "send correctly send all the messages" in {
    
    // Sends one message for each HeartbeatStatus type
    val hbStates = HeartbeatStatus.values().foreach( state => {
      // One property just to have it
      val aProp = Map("PropK" -> state.toString())
      // Build the message
      val now = System.currentTimeMillis()
      
      kProd.send(supervIdentifier.fullRunningID,state,aProp)
      val op = Try(Thread.sleep(50))
      
    })
    
    // Give some time to read all strings
    val op = Try(Thread.sleep(1000))
    
    assert(receivedStrings.size==HeartbeatStatus.values().length)
    
    val hbMessages = receivedStrings.map(str => serializer.deserializeFromString(str))
    
    assert(hbMessages.forall( msg => msg._1==supervIdentifier.fullRunningID))
    
    assert(HeartbeatStatus.values().forall( state => {
      hbMessages.filter(msg => msg._2==state).size==1
    }))
  }
  
}
