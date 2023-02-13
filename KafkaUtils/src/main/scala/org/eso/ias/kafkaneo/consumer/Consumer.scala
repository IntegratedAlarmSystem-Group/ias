package org.eso.ias.kafkaneo.consumer

import com.typesafe.scalalogging.Logger
import org.eso.ias.kafkaneo.{Consumer, ConsumerHelper, ConsumerListener, IasTopic}
import org.eso.ias.logging.IASLogger

import java.util.Collections
import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArrayList}

/**
 * Kafka consumer to get events from the BSDB.
 *
 * This consumer allows to get events from several topics: compared to
 * consumers in the [[org.eso.ias.kafkautils]] package, a process can instantiate only one consumer
 * to get events for more topics (for example IASIOs and commands).
 *
 * Features:
 *  - more listeners for each topic
 *  - collection of events are delivered to listeners
 *
 * '''Limitations''':
 *  - the consumer uses only one group.id as it belongs to only one group; this is subject to kafka strategy
 *    on assigning consumers to topic partitions depending on the group (and other consumers in the same
 *    group).
 *    In short if you want your consumer to get all events from topic 1 and all events from topic 2
 *    (most common case in IAS) then you have to ensure that the consumer is the only one consumer in the group
 *    for both topics.
 *  - the serializer/deserializer is the same for all the topics so it is the client that gets the event
 *    as it has been read from kafka and translate it into an object of the right type
 *    (for example the event is a JSON string then it must be parsed to build the java object like a command
 *    or a IASIO)
 *
 * @todo add the number of processed records per topic
 *
 * @constructor create a new kafka consumer
 * @param groupId the group.id property for the kafka consumer
 * @param id the id of the consumer for the kafka consumer
 * @param kafkaServers the string of servers and ports to connect to kafka servers
 * @param keyDeserializer the name of the class to deserialize the key
 * @param valueDeserializer the name of the class to deserialize the value
 * @param kafkaProperties Other kafka properties to allow the user to pass custom properties
 * @author Alessandro Caproni
 * @since 13.0
 */
class Consumer(
                val groupId: String,
                val id: String,
                val kafkaServers: String,
                val keyDeserializer: String = ConsumerHelper.DefaultKeyDeserializer,
                val valueDeserializer: String = ConsumerHelper.DefaultValueDeserializer,
                val kafkaProperties: Map[String, String]) {

  /**
   * The events read from the topic.
   *
   * This Consumer does not deal with serialization so we do not want to
   * force a type to keys and values at this stage.
   *
   * @param key The key
   * @param value the value
   */
  case class TopicEvent(key: Any, value: Any)

  /**
   * The listeners of events, grouped by topic
   *
   * The map is immutable but the lists (the values) are not: they are modified when new
   * listeners are added/removed typically at startup and shutdown.
   *
   * The values are synchronizedLists list so that they can be locked individually instead of using
   * a lock global for the entire Consumer class
   *
   * The map is initialized with an empty list for each possible value i.e.
   * a list for each [[IasTopic]]
   */
  private[this] val listeners: Map[IasTopic, java.util.List[ConsumerListener[_, _]]] = {
    val map = scala.collection.mutable.Map[IasTopic, java.util.List[ConsumerListener[_, _]]]()
    IasTopic.values.foreach( map.put(_, Collections.synchronizedList(new java.util.LinkedList[ConsumerListener[_, _]]())))
    map.toMap
  }

  /**
   * Adds a listener (consumer) of events published in the topic
   *
   * @param iasTopic the topic whose events must be notified to the listener
   * @param listener the listener (consumer) of events
   */
  def addListener(iasTopic: IasTopic, listener: ConsumerListener[_, _]): Unit = synchronized {
    require(Option(iasTopic).isDefined)
    require(Option(listener).isDefined)
    val topicListeners = listeners(iasTopic)
    topicListeners.synchronized {
      if (!topicListeners.contains(listener)) then topicListeners.add(listener)
    }
  }

  /**
   * Removes a listener (consumer) of events published in the topic
   *
   * @param iasTopic the topic whose events must be notified to the listener
   * @param listener the listener (consumer) to remove
   */
  def removeListener(iasTopic: IasTopic, listener: ConsumerListener[_, _]): Unit = synchronized {
    require(Option(iasTopic).isDefined)
    require(Option(listener).isDefined)
    if (listeners(iasTopic).remove(listener)) {
      Consumer.logger.debug("A listener for topic {} has been removed", iasTopic)
    } else {
      Consumer.logger.warn("A listener for topic {} could not be removed because was not in the list of listeners", iasTopic)
    }
  }
}

object Consumer {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(Consumer.getClass)
}
