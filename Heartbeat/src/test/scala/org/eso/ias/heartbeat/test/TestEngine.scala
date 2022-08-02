package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.{HbProducer, Heartbeat, HeartbeatProducerType}

import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{HbEngine, HbMsgSerializer, HeartbeatStatus}
import org.eso.ias.logging.IASLogger
import org.scalatest.flatspec.AnyFlatSpec

import scala.language.reflectiveCalls
import scala.util.Try

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
  override def init(): Unit = {
    initialzed=true
  }
  
  /** Shutdown the producer */
  override def shutdown(): Unit = {
    cleanedUp=true
  }
  
  /**
   * Push the string
   */
  override def push(hbAsString: String): Unit = {
    val str = Option(hbAsString)
    str.filter(!_.isEmpty).foreach(s => {
      logger.info("HB received {}",s)
      pushedStrings.append(s)
    })
  }
}

class TestEngine extends AnyFlatSpec {
  
  /** Fixture to build the HB engine to tests */
  trait Fixture {
    /** The ID of th esupervisor */
    val supervId = "SupervisorID"

    /** The HB of the supervisor */
    val supervHeartbeat = Heartbeat(HeartbeatProducerType.SUPERVISOR,supervId)

    val frequency = 2

    val serializer = new HbJsonSerializer

    val producer = new MockProducer(serializer)

    /**
     * The engine to test
     */
    val engine = new HbEngine(supervHeartbeat,frequency,producer)
  }
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[TestEngine])
  
  behavior of "The HB engine" 
  
  it must "init and shutdown the producer" in new Fixture {
    engine.start()
    engine.shutdown()
    
    assert(producer.initialzed)
    assert(producer.cleanedUp)
  }
  
  it must "not send HBs before initialization" in new Fixture {

    // Give time to send messages
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
        
    assert(producer.pushedStrings.isEmpty)
  }
  
  it must "send the default intial message" in new Fixture {

    logger.info("Starting engine")
    engine.start()
    
    // Give time to send one and only one message
    logger.info("Waiting...")
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    logger.info("shutting down")
    engine.shutdown()
    
    assert(producer.pushedStrings.size==1)
    val status=serializer.deserializeFromString(producer.pushedStrings(0))._2
    assert(status==engine.initialStatusDefault)
  }
  
  it must "send the intial message" in new Fixture {

    engine.start(HeartbeatStatus.RUNNING)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    engine.shutdown()
    
    assert(producer.pushedStrings.size==1)
    val status=serializer.deserializeFromString(producer.pushedStrings(0))._2
    assert(status==HeartbeatStatus.RUNNING)
  }
  
  it must "update the state" in new Fixture {
    engine.start(HeartbeatStatus.STARTING_UP)
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    engine.addProperty("key1", "prop1");
    engine.addProperty("key2", "prop2");
    engine.updateHbState(HeartbeatStatus.RUNNING)
    
    val op2 = Try(Thread.sleep(2000))
    assert(op2.isSuccess)
    
    engine.shutdown()
    
    assert(producer.pushedStrings.size==2)
    
    val deserialized1 = serializer.deserializeFromString(producer.pushedStrings(0))
    assert(deserialized1._2==HeartbeatStatus.STARTING_UP)
    assert(deserialized1._3.isEmpty)
    
    val deserialized2 = serializer.deserializeFromString(producer.pushedStrings(1))
    assert(deserialized2._2==HeartbeatStatus.RUNNING)
    assert(deserialized2._3.size==2)
  }
  
  it must "not send HBs after shutdown" in new Fixture {
    engine.start()
    
    // Give time to send one and only one message
    val op = Try(Thread.sleep(3000))
    assert(op.isSuccess)
    
    engine.updateHbState(HeartbeatStatus.RUNNING)
    engine.addProperty("key1-t2", "prop1-t2")
    
    val op2 = Try(Thread.sleep(2000))
    assert(op2.isSuccess)
    
    engine.shutdown()
    
    assert(producer.pushedStrings.size==2)
    
    val op3 = Try(Thread.sleep(5000))
    assert(op3.isSuccess)
    
    assert(producer.pushedStrings.size==2)
  }
  
  it must "send periodically send the same msg if not changed" in new Fixture {
    engine.start(HeartbeatStatus.PARTIALLY_RUNNING)
    
    // Give time to send 5 messages
    val op = Try(Thread.sleep(frequency*5000+1000))
    assert(op.isSuccess)
    
    engine.shutdown()
    
    assert(producer.pushedStrings.size==5)
    
    producer.pushedStrings.foreach( str => {
      val msg=serializer.deserializeFromString(str)._2
      assert(msg==HeartbeatStatus.PARTIALLY_RUNNING)
    })
  }
  
}
