package org.eso.ias.kafkaneo.test.consumer

import org.eso.ias.kafkaneo.IasTopic
import org.eso.ias.kafkaneo.consumer.{ConsumerListener, ConsumerListenerJ}
import org.eso.ias.logging.IASLogger
import org.scalatest.flatspec.AnyFlatSpec

import scala.jdk.javaapi.CollectionConverters
import scala.reflect.ClassTag

class ListenerTest extends AnyFlatSpec {

  class ListenerForTest[K, V](topic: IasTopic, maxCacheSize: Int)
    extends ConsumerListener[K, V](topic, maxCacheSize) {

    val records: collection.mutable.ListBuffer[KEvent] = new collection.mutable.ListBuffer[KEvent]()

    def onKafkaEvents(events: List[KEvent]): Unit = {
      logger.info("Received {} events", events.size)
      records.addAll(events)
    }
  }

  class ListenerForTestJ[K, V](topic: IasTopic,  maxCacheSize: Int)
    extends ConsumerListenerJ[K, V](topic, maxCacheSize) {

    val records: collection.mutable.ListBuffer[KEvent] = new collection.mutable.ListBuffer[KEvent]()
    def onKafkaEventsJ(events: java.util.List[KEvent]): Unit = {
      logger.info("Received {} Jevents", events.size)
      records.addAll(CollectionConverters.asScala(events))
    }
  }

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  "A listener" should "get the size and the topic" in {
    val l =  new ListenerForTest[Int, Int](IasTopic.Heartbeat, 100)
    assert(l.maxCacheSize==100)
    assert(l.iasTopic eq IasTopic.Heartbeat)
    val lj = new ListenerForTestJ[Int, Int](IasTopic.Core, 125)
    assert(lj.maxCacheSize==125)
    assert(lj.iasTopic eq IasTopic.Core)
  }

  it should "deliver the records for the scala listener" in {
    val l =  new ListenerForTest[String, String](IasTopic.Heartbeat, 100)
    val events: List[l.KEvent] = List(l.KEvent("Key1", "Value1"))
    l.internalOnKEvents(events)
    assert(l.records.size==0)

    val events2: List[l.KEvent] = List(l.KEvent("Key2", "Value2"), l.KEvent("Key3", "Value3"), l.KEvent("Key4", "Value4"))
    l.enable()
    l.internalOnKEvents(events2)
    assert(l.records.size==events2.size)
    assert(l.records.toList == events2)

    l.disable()
    l.records.clear()
    val events3: List[l.KEvent] = List(l.KEvent("Key5", "Value5"), l.KEvent("Key6", "Value6"))
    l.internalOnKEvents(events3)
    assert(l.records.isEmpty)
  }

  it should "deliver the records for the java listener" in {
    val l =  new ListenerForTestJ[String, String](IasTopic.Heartbeat, 100)
    val events: List[l.KEvent] = List(l.KEvent("Key1", "Value1"))
    l.internalOnKEvents(events)
    assert(l.records.size==0)

    val events2: List[l.KEvent] = List(l.KEvent("Key2", "Value2"), l.KEvent("Key3", "Value3"), l.KEvent("Key4", "Value4"))
    l.enable()
    l.internalOnKEvents(events2)
    assert(l.records.size==events2.size)
    assert(l.records.toList == events2)

    l.disable()
    l.records.clear()
    val events3: List[l.KEvent] = List(l.KEvent("Key5", "Value5"), l.KEvent("Key6", "Value6"))
    l.internalOnKEvents(events3)
    assert(l.records.isEmpty)
  }

  it should "pause/resume" in {
    val l = new ListenerForTest[String, String](IasTopic.Heartbeat, 5)
    val events: List[l.KEvent] = List(l.KEvent("Key1", "Value1"))
    l.enable()
    l.internalOnKEvents(events)
    assert(l.records.size == 1)
    // No events delivered when paused
    l.pause()
    val events2: List[l.KEvent] = List(l.KEvent("Key2", "Value2"))
    l.internalOnKEvents(events2)
    assert(l.records.size == 1)

    // After resuming all the events cached must be delivered to the listener
    l.resume()
    assert(l.records.size == 2)
  }

  it should "discard events when paused" in {
    val l = new ListenerForTest[String, String](IasTopic.Heartbeat, 5)
    val events: List[l.KEvent] = List(l.KEvent("Key1", "Value1"))
    l.enable()
    l.internalOnKEvents(events)
    assert(l.records.size == 1)

    l.pause()
    l.records.clear()

    // Push more items than the ones that should be stored in cache
    val events2: List[l.KEvent] = List(
      l.KEvent("Key2", "Value2"), l.KEvent("Key3", "Value3"), l.KEvent("Key4", "Value4"),
      l.KEvent("Key5", "Value5"), l.KEvent("Key6", "Value6"), l.KEvent("Key7", "Value7"),
      l.KEvent("Key8", "Value8"), l.KEvent("Key8", "Value8"), l.KEvent("Key9", "Value9"))
    l.internalOnKEvents(events2)
    l.resume()
    assert(l.records.size == 5)
    val expectedEvents: List[l.KEvent] = List(
      l.KEvent("Key6", "Value6"), l.KEvent("Key7", "Value7"),
      l.KEvent("Key8", "Value8"), l.KEvent("Key8", "Value8"), l.KEvent("Key9", "Value9"))
    assert(l.records.toList == expectedEvents)
  }


}
