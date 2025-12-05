package org.eso.ias.kafkaneo.test.consumer

import com.typesafe.scalalogging.Logger
import org.eso.ias.kafkaneo.IasTopic
import org.eso.ias.kafkaneo.consumer.ConsumerListener
import org.eso.ias.logging.IASLogger

class MockListener(topic: IasTopic) extends ConsumerListener[String, String](topic) {

  /** The events consumed from the topic */
  private val receivedEvents = new collection.mutable.ListBuffer[KEvent]()

  /** Empty the list of received events */
  def clear(): Unit = synchronized { receivedEvents.clear() }

  /** @return the events received so far */
  def getRecvEvents(): List[KEvent] = synchronized { receivedEvents.toList}

  /** @retrun the number of items received */
  def size: Int = synchronized { receivedEvents.size }

  /** @return true if the listener did not receive any event */
  def isEmpty: Boolean = synchronized { receivedEvents.isEmpty }

  /** New items has been read from the kafka topic */
  override def onKafkaEvents(events: List[KEvent]): Unit = synchronized {
    MockListener.logger.info("{} events received from topic {}: {}",
      events.size,
      iasTopic,
      events.map(e => s"v=${e.value},k=${e.key}").mkString(" "))

    receivedEvents.addAll(events)
  }
}

object MockListener {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(MockListener.getClass)
}
