package org.eso.ias.heartbeat.consumer

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import com.typesafe.scalalogging.Logger
import org.eso.ias.heartbeat.HeartbeatStatus
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.kafkautils.KafkaStringsConsumer.StringsConsumer
import org.eso.ias.kafkautils.{KafkaHelper, KafkaStringsConsumer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.ISO8601Helper

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Try}

/** The trait for the listener of HB consumed from the kafka topic */
trait  HbListener {

  /**
    * An heartbeat has been read from the HB topic
    *
    * @param tStamp The timestamp of the event
    * @param id the ID of the producer
    * @param state the state of the producer
    * @param props The properties
    */
  def hbReceived(tStamp: Long, id: String, state: HeartbeatStatus, props: Map[String, String])
}

/**
  * The HbKafkaConsumer gets HBs from the hb kakfa topic and notifies the registered listeners
  *
  * HbKafkaConsumer is the listener of the [[KafkaStringsConsumer]] and forward the events
  * to registered listeners.
  * Events received from the topic are stored in a temporary buffer and sent to the listeners
  * asynchronously
  *
  * @param brokers Kafka brokers to connect to
  * @param consumerId The id for the consumer
  */
class HbKafkaConsumer(brokers: String, consumerId: String)
  extends KafkaStringsConsumer(brokers, KafkaHelper.HEARTBEAT_TOPIC_NAME, consumerId)
    with StringsConsumer
    with Runnable {

  /** The message read from the kafka topic */
  case class HbMsg(id: String, status: HeartbeatStatus, props: Map[String, String], timestamp: Long) {
    require(Option(id).isDefined && id.nonEmpty, "Invalid identifier")
    require(Option(status).isDefined,"Invalid empty status")
    require(Option(props).isDefined,"Invalid empty props")

    override def toString: String = {
      val s = new StringBuilder("HB message [id=")
      s.append(id)
      s.append(", status=")
      s.append(status)
      s.append(", at ")
      s.append(ISO8601Helper.getTimestamp(timestamp))
      if (Option(props).isDefined && props.nonEmpty) {
        s.append(", props={")
        val propsStr=for {
          (k,v) <- props
          s = k+'='+v
        } yield s
        s.append(propsStr.mkString(","))
        s.append('}')
      }
      s.append(']')
      s.toString()
    }
  }

  /** The buffer of events read from the kafka topic */
  private val buffer: LinkedBlockingQueue[HbMsg] = new LinkedBlockingQueue[HbMsg]()

  /** The deserializer to convert JSON strings into the java pojo */
  private val deserializer = new HbJsonSerializer

  /** The listener to notify of HB events */
  private val listeners: mutable.ListBuffer[HbListener] = ListBuffer.empty

  /** Signal that the object has been closed */
  private val closed = new AtomicBoolean(false)

  private val thread = new AtomicReference[Thread]()

  /**
    * Add a lsitener to be notifiwed of HB events
    * @param listener the not empty listener to add
    */
  def addListener(listener: HbListener): Unit = {
    require(Option(listener).isDefined,"Cannot add an empty listener")
    listeners.synchronized{
      listeners.append(listener)
    }
  }

  /**
    * The thread that gets events from the queue and notify the listeners
    */
  override def run(): Unit = {
    HbKafkaConsumer.logger.info("Thread started")
     while (!closed.get()) {
       Try{
         val msg = Option(buffer.poll(1, TimeUnit.HOURS))
         msg.foreach(notifyListeners(_))
       }
     }
    HbKafkaConsumer.logger.info("Thread terminated")
  }

  /**
    * Notify all registered listener of a new events
    * read from the kafka topic
    *
    * @param event The HB event
    */
  def notifyListeners(event: HbMsg): Unit = {
    require(Option(event).isDefined)
    listeners.synchronized {
      listeners.foreach(l => {
        Try(l.hbReceived(event.timestamp,event.id,event.status,event.props)) match {
          case Failure(exception) =>  HbKafkaConsumer.logger.error("Error notifying listener",exception)
          case _ => HbKafkaConsumer.logger.debug("HB event successfully notified: ",event.toString)
        }
      })
    }
  }

  /**
    * One or more HBs has been read from the kafka topic
    *
    * The events are added to the buffer
    *
    * @param strings The strings read from the kafka topic
    */
  def stringsReceived(strings: util.Collection[String]) {
    strings.forEach(str =>  {
      val hbMessage = deserializer.deserializeFromString(str)
      val id = hbMessage._1
      val status: HeartbeatStatus = hbMessage._2
      val props: Map[String, String] = hbMessage._3
      val tStamp = hbMessage._4
      buffer.offer(HbMsg(id,status,props,tStamp))
    })
  }

  /** Start getting events */
  def start(): Unit = {
    require(Option(thread.get()).isEmpty,"Thread already started")
    thread.set(new Thread(this))
    thread.get.setDaemon(true)
    thread.get().start()
    HbKafkaConsumer.logger.debug("Thread started")
    startGettingEvents(this,KafkaStringsConsumer.StartPosition.END)
  }

  /** Stop getting events */
  def sutdown(): Unit = {
    closed.set(true)
    Option(thread.get()).foreach(_.interrupt())
    super.tearDown()
  }


}

/** Companion object */
object HbKafkaConsumer {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(HbKafkaConsumer.getClass)
}