package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.HbProducer
import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import java.util.concurrent.TimeUnit
import org.eso.ias.heartbeat.HbEngine
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
    
    val serializer = new HbJsonSerializer
    
    val producer = new MockProducer(serializer)
    
    /**
     * The engine to test
     */
    val engine = new HbEngine(supervIdentifier.fullRunningID,frequency,producer)
    
  }
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[TestEngine])
  
  behavior of "The HB engine" 
  
  it must "init and shutdown the producer" in {
    val f = fixture
    
    f.engine.start()
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
  
  it must "send the default intial message" in {
    val f = fixture
    
    logger.info("Starting engine")
    f.engine.start()
    
    // Give time to send one and only one message
    logger.info("Waiting...")
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    logger.info("shutting down")
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==1)
    val status=f.serializer.deserializeFromString(f.producer.pushedStrings(0))._2
    assert(status==f.engine.initialStatusDefault)
  }
  
  it must "send the intial message" in {
    val f = fixture
    
    f.engine.start(HeartbeatStatus.RUNNING)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==1)
    val status=f.serializer.deserializeFromString(f.producer.pushedStrings(0))._2
    assert(status==HeartbeatStatus.RUNNING)
  }
  
  it must "update the state" in {
    
    val f = fixture
    
    f.engine.start(HeartbeatStatus.STARTING_UP)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    f.engine.addProperty("key1", "prop1");
    f.engine.addProperty("key2", "prop2");
    f.engine.updateHbState(HeartbeatStatus.RUNNING)
    
    val op2 = Try(Thread.sleep(2000))
    assert(op2.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==2)
    
    val deserialized1 = f.serializer.deserializeFromString(f.producer.pushedStrings(0))  
    assert(deserialized1._2==HeartbeatStatus.STARTING_UP)
    assert(deserialized1._3.isEmpty)
    
    val deserialized2 = f.serializer.deserializeFromString(f.producer.pushedStrings(1))
    assert(deserialized2._2==HeartbeatStatus.RUNNING)
    assert(deserialized2._3.size==2)
  }
  
  it must "not send HBs after shutdown" in {
    val f = fixture
    
    f.engine.start()
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    f.engine.updateHbState(HeartbeatStatus.RUNNING)
    f.engine.addProperty("key1-t2", "prop1-t2")
    
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
    
    f.engine.start(HeartbeatStatus.PARTIALLY_RUNNING)
    
    // Give time to send 5 messages
    val op = Try(Thread.sleep(f.frequency*5000+1000))
    assert(op.isSuccess)
    
    f.engine.shutdown()
    
    assert(f.producer.pushedStrings.size==5)
    
    f.producer.pushedStrings.foreach( str => {
      val msg=f.serializer.deserializeFromString(str)._2
      assert(msg==HeartbeatStatus.PARTIALLY_RUNNING)
    })
  }
  
}
