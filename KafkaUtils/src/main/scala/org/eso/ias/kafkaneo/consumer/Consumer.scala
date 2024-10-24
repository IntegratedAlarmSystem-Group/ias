package org.eso.ias.kafkaneo.consumer

import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.eso.ias.kafkaneo.IasTopic
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.logging.IASLogger

import java.time.Duration
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{ArrayList, Collection, Collections}
import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArrayList}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters
import scala.util.control.Breaks.{break, breakable}
import scala.compiletime.uninitialized

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
 *  - the serializer/deserializer is the same for all the topics for example a StringSerialize/StringDeserializer
 *    (for example the event is a JSON string, then it must be parsed to build the java object like a command
 *    or a IASIO)
 *
 * @tparam K The type of the key
 * @tparam V The type of the value
 *
 * @constructor create a new kafka consumer
 * @param id the id of the consumer for the kafka consumer
 * @param groupId the group.id property for the kafka consumer
 * @param kafkaServers the string of servers and ports to connect to kafka servers
 * @param keyDeserializer the name of the class to deserialize the key
 * @param valueDeserializer the name of the class to deserialize the value
 * @param startReadingPos The position in the kafka stream to start reading message,
 *                        default to the end of the stream
 * @param kafkaProperties Other kafka properties to allow the user to pass custom properties
 * @author Alessandro Caproni
 * @since 13.0
 */
class Consumer[K, V](
                val id: String,
                val groupId: String,
                val kafkaServers: String = KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                val keyDeserializer: String = ConsumerHelper.DefaultKeyDeserializer,
                val valueDeserializer: String = ConsumerHelper.DefaultValueDeserializer,
                val startReadingPos: StartReadingPos = StartReadingPos.End,
                val kafkaProperties: Map[String, String] = Map.empty) extends Thread with ConsumerRebalanceListener {

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
  private val listeners: Map[IasTopic, java.util.List[ConsumerListener[K, V]]] = {
    val map = scala.collection.mutable.Map[IasTopic, java.util.List[ConsumerListener[K, V]]]()
    IasTopic.values.foreach( map.put(_, new java.util.LinkedList[ConsumerListener[K, V]]()))
    map.toMap
  }

  /** The time, in milliseconds, spent waiting in poll if data is not available in the buffer */
  private val POLLING_TIMEOUT = Duration.ofSeconds(15)

  /** The Kafka consumer getting events from the kafka topic   */
  val consumer: KafkaConsumer[K, V] = {
    val props = ConsumerHelper.setupProps(groupId, kafkaProperties, kafkaServers, keyDeserializer, valueDeserializer)
    new KafkaConsumer[K,V](props)
  }

  /** Signal if the consumer has been started */
  val isStarted: AtomicBoolean = new AtomicBoolean(false)

  /** Signal if the consumer has been closed */
  val isClosed: AtomicBoolean = new AtomicBoolean(false)

  /** The thread consuming BSDB events */
  private var consumerThread: Thread = uninitialized

  /** A flag signaling that the shutdown from the hook is in progress */
  private val isShuttingDown = new AtomicBoolean(false)

  /** The tread to shutdown the consumer if not done by the user */
  val shutDownThread = new Thread(() => { isShuttingDown.set(true); close() })

  /** The statistics */
  val stats = new ConsumersStatistics(id)

  /** The topics from where this consumer get events from (i.e. the topics for which there are listeners) */
  def topicsOfListeners(): List[IasTopic] = synchronized {
    (for {
      k <- listeners.keys if !listeners(k).isEmpty
    } yield k).toList
  }

  /**
   * Adds a listener (consumer) of events published in the topic
   *
   * @param listener the listener (consumer) of events
   */
  def addListener(listener: ConsumerListener[K, V]): Unit = synchronized {
    require(Option(listener).isDefined)
    val topicListeners = listeners(listener.iasTopic)
    if !topicListeners.contains(listener) then topicListeners.add(listener)
  }

  /**
   * Removes a listener (consumer) of events published in the topic
   *
   * @param listener the listener (consumer) to remove
   * @return true if the listener was in the container, false otherwise
   */
  def removeListener( listener: ConsumerListener[K, V]): Boolean = synchronized {
    require(Option(listener).isDefined)
    listeners(listener.iasTopic).remove(listener)
  }

  /**
   * Get and return the listeners for the given topic
   *
   * @param topic the topic
   * @return the listeners for the passed topic
   */
  private[kafkaneo] def getListeners(topic: IasTopic): List[ConsumerListener[K,V]] = synchronized {
    CollectionConverters.asScala(listeners(topic)).toList
  }

  /**
   * Subscribe to the kafka topics for which there are listeners waiting to get events
   * and unsubscribe to topics for which there are no listeners waiting to get events.
   *
   * The former happens when new listeners are added to the consumer,
   * the latter when listeners are removed from the consumer
   */
  private def subscribeToTopics(): Unit = synchronized {
    val topicsToSubscribe: Set[String] = topicsOfListeners().map(_.kafkaTopicName).toSet
    Consumer.logger.debug("Consumer [{}] needs to get events from the following topics: {}",id,topicsToSubscribe.mkString)
    val assignedTopicPartitions: Set[TopicPartition] = CollectionConverters.asScala(consumer.assignment()).toSet
    Consumer.logger.debug("Consumer [{}] is assigned to the following topic/partition {}",
      id,
      assignedTopicPartitions.map(tp => s"${tp.topic()}/${tp.partition()}".mkString))
    val assignedTopics = assignedTopicPartitions.map(_.topic())

    if (topicsToSubscribe!=assignedTopics) {
      Consumer.logger.debug("Consumer [{}] is going to subscribe to a new set of topics...",id)
      consumer.subscribe(CollectionConverters.asJava(topicsToSubscribe), this)
      Consumer.logger.info("Consumer [{}] subscribed {} topics",id, topicsToSubscribe.mkString)
    } else {
      Consumer.logger.info("Consumer [{}] already subscribed to the topics requested by the listeners: ",id, topicsToSubscribe.mkString)
    }
  }

  /** Start the consumer: connect to the BSDB and start getting events from the thread */
  def init(): Unit = synchronized {
    if (isClosed.get) {
      Consumer.logger.error("Cannot open a closed consumer")
    } else {
      // Subscribe to the topics
      subscribeToTopics()
      // Start the consumer thread
      Consumer.logger.info("Starting the consumer {}", id)
      consumerThread = new Thread(this, s"ConsumerThread:$id")
      consumerThread.setDaemon(true)
      consumerThread.start()
      Runtime.getRuntime.addShutdownHook(shutDownThread)
      isStarted.set(true)
    }
  }

  /**  Stop getting events, close the connection with the BSDB and close the thread */
  def close(): Unit = synchronized {
    if (isClosed.get()) {
      Consumer.logger.warn("Consumer {} already closed", id)
    } else if (!isStarted.get()) {
      Consumer.logger.error("Can't close the consumer {} that has never been started", id)
    } else {
      isClosed.set(true)
      Consumer.logger.info("Closing the consumer {}", id)
      if (!isShuttingDown.get())
        try Runtime.getRuntime.removeShutdownHook(shutDownThread)
        catch {
          case e: IllegalStateException =>
        } // Already shutting down
        consumer.wakeup()
        try {
          consumerThread.join(60000)
          if (consumerThread.isAlive) Consumer.logger.error("The thread of [{}] to get events did not exit", id)
        } catch {
          case ie: InterruptedException =>
            Thread.currentThread.interrupt()
        }
    }
  }

  /**
   * Called before the rebalancing starts and after the consumer stopped consuming events.
   *
   * @param parts The list of partitions that were assigned to the consumer and now need to be revoked (may not
   *              include all currently assigned partitions, i.e. there may still be some partitions left)
   */
  override def onPartitionsRevoked(parts: util.Collection[TopicPartition]): Unit = {
    val partitions = CollectionConverters.asScala(parts)
    // isPartitionAssigned.set(!consumer.assignment.isEmpty)
    if (partitions.isEmpty) Consumer.logger.info("Consumer [{}]: no partitions need to be revoked", id)
    else Consumer.logger.info("Consumer [{}]: {} partition(s) need to be revoked {}", id, partitions.size, partitions.mkString(", "))
  }

  /**
   * Called after partitions have been reassigned but before the consumer starts consuming messages
   *
   * @param parts The list of partitions that are now assigned to the consumer (previously owned partitions will
   *              NOT be included, i.e. this list will only include newly added partitions)
   */
  override def onPartitionsAssigned(parts: util.Collection[TopicPartition]): Unit = {
     val partitions = CollectionConverters.asScala(parts)
     //isPartitionAssigned.set(!consumer.assignment.isEmpty)
     if (!partitions.isEmpty) {
      Consumer.logger.info("Consumer [{}]: {} new partitions assigned {}", id, partitions.size, partitions.mkString(", "))
      if (startReadingPos==StartReadingPos.Begin) {
        Consumer.logger.debug("Consumer [{}]: seeking to the beginning", id)
        consumer.seekToBeginning(parts)
      }
      else if (startReadingPos==StartReadingPos.End) {
        Consumer.logger.debug("Consumer [{}]: seeking to the end", id)
        consumer.seekToEnd(parts)
      }
     } else Consumer.logger.info("Consumer [{}]: no new partitions assigned", id)
  }

  /**
   * @param parts The list of partitions that were assigned to the consumer and now have been reassigned
   *              to other consumers. With the current protocol this will always include all of the consumer's
   *              previously assigned partitions, but this may change in future protocols (ie there would still
   *              be some partitions left)
   */
  override def onPartitionsLost(parts: util.Collection[TopicPartition]): Unit = {
    val partitions = CollectionConverters.asScala(parts)
    Consumer.logger.info("Consumer [{}]: {} partitions reassigned to other consumers: {}", id, partitions.size, partitions.mkString(", "))
    // isPartitionAssigned.set(!consumer.assignment.isEmpty)
  }

  /**
   * Dispatch the events read from the kafka topic to the listeners
   *
   * @param events the events read from the topics
   */
  def dispatchEvents(events: List[ConsumerRecord[K, V]]): Unit = {
    Consumer.logger.debug("Consumer {} dispatching {} events to the listeners", id , events.size)

    for (topic <- IasTopic.values.map(_.kafkaTopicName)) {
      val eventsToDispatch = events.filter(_.topic()==topic)
      val listenersToDispatchEvents: Option[util.List[ConsumerListener[K, V]]] = IasTopic.fromKafkaTopicName(topic).map(t => listeners(t))
      Consumer.logger.debug("Consumer {}: {} events to dispatch for topic {} to {} listeners",
        id,
        eventsToDispatch.size,
        topic,
        listenersToDispatchEvents.map(_.size()).getOrElse(0))

      listenersToDispatchEvents.foreach(l => // Is there a listener?
        l.forEach(consListener => {
          val events = eventsToDispatch.map( e => consListener.KEvent(e.key(), e.value()))
          try consListener.internalOnKEvents(events)
          catch
            case t: Throwable => Consumer.logger.error("Consumer [{}] got en error dispatching {} events to a listener of {}",
              id, events.size, consListener.iasTopic,t)
        }))
    }

  }

  /**
   * The thread to poll data from the topic
   *
   * @see java.lang.Runnable#run()
   */
  override def run(): Unit = {
    Consumer.logger.debug("Consumer [{}]: thread to poll events from the BSDB started", id)
    while (!isClosed.get) {
      breakable {
        try {
          val records: Iterable[ConsumerRecord[K, V]] = CollectionConverters.asScala(consumer.poll(POLLING_TIMEOUT))
          Consumer.logger.debug("Consumer [{}] got {} records from the subscribed topics", id, records.size)
          dispatchEvents(records.toList)
        } catch {
          case we: WakeupException =>
            if (!isClosed.get) { // Ignore the exception when closing
              Consumer.logger.warn("Consumer [{}]: no values read from the topics in the past {} seconds", id, POLLING_TIMEOUT.getSeconds)
            }
            break
          case t: Throwable =>
            Consumer.logger.error("Consumer [{}] got an exception while getting kafka events", id, t)
            break
        }
      }
    }
    Consumer.logger.debug("Consumer [{}]: closing the kafka consumer", id)
    consumer.close()
    Consumer.logger.info("Consumer [{}]: thread to get events from topics terminated", id)
  }
}

object Consumer {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(Consumer.getClass)
}
