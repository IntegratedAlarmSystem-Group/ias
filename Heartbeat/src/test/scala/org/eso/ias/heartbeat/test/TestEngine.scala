package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.{HbProducer, Heartbeat, HeartbeatProducerType}

import java.lang.System
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
  
  /** Flag the call of the initialized method */
  var initialzed = false
  
  
  /** Flag the call of the shutdown method */
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
      pushedStrings.append(s)
      logger.info("HB received from the engine: {}; {} HBs received so far", s, pushedStrings.size)
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

    val frequency = 2 // Send HBs every 2 seconds

    val serializer = new HbJsonSerializer

    val producer = new MockProducer(serializer)

    /**
     * The engine to test
     */
    val engine = new HbEngine(supervHeartbeat,frequency,producer)

    /**
    * Wait until the HB engine sends an HB to the producer
    * or the timeout elapses.
    * This function is meant to avoid active sleeps in the tests.
    *
    * @param timeout The time (msec) to wait for the HB
    * @return None if no HB has been received before the timeout elapses;
    *         Some(str) if the HB has been received
    */
    def waitHb(timeout: Int): Option[String] = {
      require(timeout>0)
      val endTime = System.currentTimeMillis()+timeout
      val actualSize = producer.pushedStrings.size
      val waitTimeInterval = 100 // Frequency to check if a new HB arrived
      while (System.currentTimeMillis()<endTime) {
        if (producer.pushedStrings.size>actualSize) {
          return Some(producer.pushedStrings.last)
        }
        Thread.sleep(waitTimeInterval)
      }
      return None
    }
  }
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[TestEngine])
  
  behavior of "The HB engine" 
  
  it must "init and shutdown the producer" in new Fixture {
    assert(!producer.initialzed)
    assert(!producer.cleanedUp)

    engine.start()
    assert(producer.initialzed)
    assert(!producer.cleanedUp)

    engine.shutdown()
    
    assert(producer.initialzed)
    assert(producer.cleanedUp)
  }
  
  it must "not send HBs before initialization" in new Fixture {

    val hb = waitHb(3000)
    assert(hb.isEmpty)
  }
  
  it must "send the default intial message" in new Fixture {

    logger.info("Starting engine")
    engine.start()
    
    // Wait until the engine sends the first HB
    logger.info("Waiting...")
    val hb = waitHb(3000)
    assert(!hb.isEmpty)
    
    logger.info("shutting down")
    engine.shutdown()
    
    val status=serializer.deserializeFromString(hb.get)._2
    assert(status==engine.initialStatusDefault)
  }
  
  it must "send the intial message" in new Fixture {

    engine.start(HeartbeatStatus.RUNNING)

    // Wait until the engine sends the first HB
    logger.info("Waiting...")
    val hb = waitHb(3000)
    assert(!hb.isEmpty)
    
    engine.shutdown()
    
    val status=serializer.deserializeFromString(hb.get)._2
    assert(status==HeartbeatStatus.RUNNING)
  }
  
  it must "update the state" in new Fixture {
    logger.info("Test the updating of the state")
    engine.start(HeartbeatStatus.STARTING_UP)

    // Wait until the engine sends the first HB
    logger.info("Waiting...")
    val hb = waitHb(3000)
    assert(!hb.isEmpty)

    val actualSize = producer.pushedStrings.size
    
    // Changes the state again: this must be notified immediately
    engine.addProperty("key1", "prop1");
    engine.addProperty("key2", "prop2");
    engine.updateHbState(HeartbeatStatus.RUNNING)
    assert(producer.pushedStrings.size==actualSize+1)
    
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
    logger.info(" Test if the HB engine stops sending HBs after shutdown")
    engine.start()
    
    // Wait until the engine sends the first HB
    logger.info("Waiting...")
    val hb = waitHb(3000)
    assert(!hb.isEmpty)

    val actualSize = producer.pushedStrings.size
    engine.addProperty("key1-t2", "prop1-t2")
    engine.updateHbState(HeartbeatStatus.RUNNING)
    assert(producer.pushedStrings.size==actualSize+1)
    
    logger.debug(" Shutting down the HB engine")
    engine.shutdown()

    // This HB should never arrive because the engine has been shut down
    val hb2 = waitHb(3000)
    assert(hb2.isEmpty)
  }
  
  it must "send periodically send the same msg if not changed" in new Fixture {
    logger.info(" Testing periodic sending of HBs")
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
