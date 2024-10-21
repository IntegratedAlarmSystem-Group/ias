package org.eso.ias.kafkaneo.consumer

import com.typesafe.scalalogging.Logger
import org.eso.ias.kafkaneo.IasTopic
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.CircularBuffer

import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.javaapi.CollectionConverters
import scala.reflect.ClassTag

/**
 * The listener of Kafka events
 *
 * The listener must be enabled to get events.
 * When paused, it cache up to [[maxCacheSize]] items: when the cache is full, oldest events are removed
 * to leave room for new events
 *
 * @tparam K The type of the key
 * @tparam V The type of the value
 *
 * @param maxCacheSize: max number of events cached when the listener is paused
 */
trait ConsumerListener[K, V](val iasTopic: IasTopic, val maxCacheSize: Int = ConsumerListener.CacheSize) {

  /**
   * Each event read from Kafka is composed of the key and the value
   * @param key the key
   * @param value the value
   */
  case class KEvent(key: K, value: V)

  /** The cache to store events when the listener is paused */
  private lazy val cache = CircularBuffer[KEvent](maxCacheSize)

  /**
   * If true the events are forwarded to the listener
   * otherwise they are discarded
   */
  private val enabled = new AtomicBoolean(false)

  /**
   * If false the events are forwarded to the listener
   * otherwise they are cached and dispatched as soon as
   * the listener is resumed
   */
  private val paused = new AtomicBoolean(false)

  /** Pause the listener */
  def pause(): Unit = paused.set(true)

  /** Pause the listener */
  def resume(): Unit = synchronized {
    paused.set(false)
    internalOnKEvents(cache.getAll())
    cache.clear()
  }

  /** Enable the sending of events to the listener (when disabled events are discarded) */
  def enable(): Unit = enabled.set(true)

  /** Disable the sending of events (events are discarded) */
  def disable(): Unit = enabled.set(false)

  /**
   * This method is called by the consumer when new events arrive:
   * if the listener is enabled and not paused, the events are forwarded
   * to the listener by the [[onKafkaEvents()]]
   */
  private[kafkaneo] final def internalOnKEvents(events: List[KEvent]): Unit = synchronized {
    ConsumerListener.logger.debug("Internally dispatching {} events", events.size)
    (enabled.get(), paused.get()) match {
      case (false, _) => ConsumerListener.logger.warn("No events dispatched as the listeners has not been enabled")
      case (true, false) => onKafkaEvents(events)
      case (true, true) =>  cacheEvents(events)
    }
  }

  /** Cache the events when the listener is paused */
  private def cacheEvents(events: List[KEvent]): Unit = {
    assert(paused.get(), "The listener shall be paused to cache events")
    cache.putAll(events)
  }

  /**
   * Process events
   *
   * @param events the events read from Kafka
   */
  def onKafkaEvents(events: List[KEvent]): Unit
}



/**
 * The consumer of Kafka events specialized for java consumers
 *
 * This consumer provides the same features of [[ConsumerListener]]
 * but converts scala list of events into java list of events
 * before sending the events to the listener
 *
 * @tparam K The type of the key
 * @tparam V The type of the value
 */
abstract class ConsumerListenerJ[K, V](topic: IasTopic, sz: Int = ConsumerListener.CacheSize)
  extends ConsumerListener[K, V](topic, sz) {

  /**
   * Process events
   *
   * @param events the events read from Kafka
   */
  def onKafkaEventsJ(events: java.util.List[KEvent]): Unit

  override def onKafkaEvents(events: List[KEvent]): Unit = {
    onKafkaEventsJ(CollectionConverters.asJava(events))
  }
}

object ConsumerListener {

  /** The name of the property to customize the size of the cache of the listener */
  val DefaultCacheSizePropName = "org.eso.ias.kafkaneo.consumerlistener.cachesize"

  /** The size of the cache to store events when the consumer is paused */
  val DefaultCacheSize: Int = 256

  /** The size of the cache from the system properties or the default */
  val CacheSize: Int = Option[Int](Integer.getInteger(DefaultCacheSizePropName)).getOrElse(DefaultCacheSize)

  /** The logger */
  val logger: Logger = IASLogger.getLogger(ConsumerListener.getClass)

}
