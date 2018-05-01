package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.HbProducer
import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import java.util.concurrent.TimeUnit
import org.eso.ias.heartbeat.HbEngine
import org.eso.ias.heartbeat.HbMessage
import org.eso.ias.heartbeat.HeartbeatStatus
import scala.util.Try
import org.eso.ias.logging.IASLogger
import org.eso.ias.heartbeat.HbMsgSerializer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.scalatest.FlatSpec
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType

/**
 * A mock producer to record what the Engine sends
 * to the framework
 */
class MockProducer(serializer: HbMsgSerializer) extends HbProducer(serializer) {
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[MockProducer])
  
  /**
   * The strings sent to the framework
   */
  val pushedStrings: ArrayBuffer[String] = new ArrayBuffer
  
  /** Flagt he call of initialized method */
  var initialzed = false
  
  
  /** Flagt he call of shutdown method */
  var cleanedUp = false
  
  /** Initialize the producer */
  override def init() {
    initialzed=true
  }
  
  /** Shutdown the producer */
  override def shutdown() {
    cleanedUp=true
  }
  
  /**
   * Push the string
   */
  override def push(hbAsString: String) {
    val str = Option(hbAsString)
    str.filter(!_.isEmpty).foreach(s => {
      logger.info("HB received {}",s)
      pushedStrings.append(s)
    })
  }
  
}

class TestEngine extends FlatSpec {
  
  /** Fixture to build the HB engine to tests */
  def fixture =
    new {
    
    /** The identifier of the supervisor */
    val supervIdentifier = new Identifier("SupervisorID", IdentifierType.SUPERVISOR, None)
    
    val frequency = 2
    val tUnit = TimeUnit.SECONDS
    
    val serializer = new HbJsonSerializer
    
    val producer = new MockProducer(serializer)
    
    /**
     * The engine to test
     */
    val engine = new HbEngine(frequency,tUnit,producer)
    
  }
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[TestEngine])
  
  behavior of "The HB engine" 
  
  it must "init and shutdown the producer" in {
    val f = fixture
    val  initialMessage = new HbMessage(f.supervIdentifier,HeartbeatStatus.STARTING_UP)
    
    f.engine.start(initialMessage)
    f.engine.shutdown()
    
    assert(f.producer.initialzed)
    assert(f.producer.cleanedUp)
  }
  
  it must "not send HBs before initialization" in {
    val f = fixture
    
    // Give time to send messages
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
        
    assert(f.producer.pushedStrings.isEmpty)
  }
  
  it must "send the intial message" in {
    val f = fixture
    val  initialMessage = new HbMessage(f.supervIdentifier,HeartbeatStatus.STARTING_UP)
    
    f.engine.start(initialMessage)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==1)
    val msg1=f.serializer.deserializeFromString(f.producer.pushedStrings(0))._1
    assert(msg1==initialMessage)
  }
  
  it must "update the state" in {
    
    val f = fixture
    val  initialMessage = new HbMessage(f.supervIdentifier,HeartbeatStatus.STARTING_UP)
    
    f.engine.start(initialMessage)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    val addProps = Map("key1" -> "prop1", "key2" -> "prop2","key3" -> "prop3")
    val newMessage = initialMessage.setHbState(HeartbeatStatus.RUNNING).setHbProps(addProps)
    f.engine.updateHbState(newMessage)
    
    val op2 = Try(Thread.sleep(2000))
    assert(op2.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==2)
    
    val msg1=f.serializer.deserializeFromString(f.producer.pushedStrings(0))._1
    assert(msg1==initialMessage)
    val msg2=f.serializer.deserializeFromString(f.producer.pushedStrings(1))._1
    assert(msg2==newMessage)
  }
  
  it must "not send HBs after shutdown" in {
    val f = fixture
    val  initialMessage = new HbMessage(f.supervIdentifier,HeartbeatStatus.STARTING_UP)
    
    f.engine.start(initialMessage)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    val addProps = Map("key1-t2" -> "prop1-t2")
    val newMessage = initialMessage.setHbState(HeartbeatStatus.RUNNING).setHbProps(addProps)
    f.engine.updateHbState(newMessage)
    
    val op2 = Try(Thread.sleep(2000))
    assert(op2.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==2)
    
    val op3 = Try(Thread.sleep(5000))
    assert(op3.isSuccess)
    
    assert(f.producer.pushedStrings.size==2)
  }
  
  it must "send periodically send the same msg if not changed" in {
    val f = fixture
    val  initialMessage = new HbMessage(f.supervIdentifier,HeartbeatStatus.STARTING_UP)
    
    f.engine.start(initialMessage)
    
    // Give time to send 5 messages
    val op = Try(Thread.sleep(f.frequency*5000+1000))
    assert(op.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==5)
    
    f.producer.pushedStrings.foreach( str => {
      val msg=f.serializer.deserializeFromString(str)._1
      assert(msg==initialMessage)
    })
  }
  
}