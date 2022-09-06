package org.eso.ias.heartbeat.report

import com.typesafe.scalalogging.Logger
import org.eso.ias.heartbeat.consumer.{HbKafkaConsumer, HbListener, HbMsg}
import org.eso.ias.heartbeat.{HeartbeatProducerType, HeartbeatStatus}
import org.eso.ias.logging.IASLogger

import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Timer, TimerTask}
import scala.collection.mutable.{ArrayBuffer, Map as MutableMap}

/**
 * HbsCollector gets and collects the HBs received between invocations of
 * startCollectingHbs and stopCollectingHBs.
 *
 * HBs older than a given TTL (>0) are automatically removed from the container
 * so that dead processes will disappear after they stop sending HBs.
 * The reception time of the HB is checked against the TTL in a Timer task.
 *
 * TODO: There is some overlap with the HbMonitor here:
 *       let HbMonitor re-use this class
 *
 * @param brokers Kafka brokers to connect to
 * @param consumerId The id for the kafka consumer
 * @param ttl time to leave (msec>=0) disabled by default,
 *             if ttl>0, HBs older then ttl are automatically removed from the container
 *             if ttl<0, received HBs are never removed from the container
 *             It makes sense to link TTL to the HB period set in the CDB ("hbFrequency")
 * @param ttlCheckTime the period (msec>0) to check for old HBs
 */
class HbsCollector(
                    brokers: String,
                    consumerId: String,
                    val ttl: Long = 0,
                    val ttlCheckTime: Long = 1000) extends TimerTask with HbListener {
  require(ttlCheckTime>0,"Invalid TTL period")

  /** The consumer of HBs */
  val hbConsumer = new HbKafkaConsumer(brokers, consumerId)

  /** The key for the map of HB is composed of the type and the id of the tool */
  case class CompoundKey(hbProducerType: HeartbeatProducerType, id: String)

  /** The map of the received HBs */
  val hbs: MutableMap[CompoundKey, HbMsg] = MutableMap[CompoundKey, HbMsg]()

  /** The timer to remove old HBs */
  val timer: Timer = new Timer("HbsCollectorTimer", true)

  /** Signal if the HbsCollector is collecting HBs */
  val collectingHbs = new AtomicBoolean(false)

  /**
   * Set to true when the object has been initialized
   * i.e. connected to the HB Kafka topic and the time activated (if ttl>0)
   */
  val initialized = new AtomicBoolean(false)

  /** The task run by the timer that removes the HBs older than the ttl */
  def run(): Unit = synchronized {
    assert(ttl>0, "The timer task should not run if ttl<=0")
    val oldestTStamp = System.currentTimeMillis() - ttl
    val hbsToRemove: MutableMap[CompoundKey, HbMsg] = hbs.filter((key, value) => value.timestamp<oldestTStamp)
    hbsToRemove.keys.foreach(k => {
      HbsCollector.logger.debug(s"Removing old HBs of a ${k.hbProducerType} with id ${k.id}")
      hbs -= k
    })
  }

  /**
   * Get and return the HBs in the map of the passed type
   *
   * @param hbProdType the type of the producers
   */
  def getHbsOfType(hbProdType: HeartbeatProducerType): List[HbMsg] = synchronized {
    val hbsToReturn: MutableMap[CompoundKey, HbMsg] = hbs.filter((key, value) => key.hbProducerType==hbProdType)
    hbsToReturn.values.toList
  }

  /**
   * Get and return the HB of the tool with the given type and id
   *
   * @param hbProdType the type of the producer
   * @param id the ID of the tool
   */
  def getHbOf(hbProdType: HeartbeatProducerType, id: String): Option[HbMsg] = synchronized {
    require(id.nonEmpty, "The ID cannot be empty")
    hbs.get(CompoundKey(hbProdType, id))
  }

  /** Connect to the kafka brokers. */
  def setup(kafkaBrokers: String, consumerID: String): Unit = {
    HbsCollector.logger.debug("Initializing...")
    hbConsumer.addListener(this)
    hbConsumer.start()
    if (ttl>0) {
      HbsCollector.logger.debug("Starting the timer task")
      timer.scheduleAtFixedRate(this, ttlCheckTime, ttlCheckTime)
    }
    initialized.set(true)
    HbsCollector.logger.debug("Initialized")
  }

  /** Disconnect the consumer */
  def shutdown(): Unit = {
    HbsCollector.logger.debug("Shutting down...")
    if (ttl>0) {
      HbsCollector.logger.debug("Terminating the timer task")
      timer.cancel()
    }
    hbConsumer.shutdown()
    HbsCollector.logger.debug("Shutdown")
  }

  /** Starts collecting the HBs. */
  def startCollectingHbs(): Unit = {
    require(initialized.get(), "The collector must be initialized before getting HBs")
    HbsCollector.logger.debug("Start getting HBs")
    collectingHbs.set(true)
  }

  /**
   * Stops collecting the HBs.
   */
  def stopCollectingHbs(): Unit = {
    collectingHbs.set(false)
    HbsCollector.logger.debug("Stopped getting HBs")
  }

  /**
   * Remove all the HBs collected so far
   */
  def clear(): Unit = synchronized {
    hbs.clear()
  }

  /**
   * An heartbeat has been read from the HB topic: it is added to the container
   *
   * @param hbMsg The HB message
   */
  def hbReceived(hbMsg: HbMsg): Unit = synchronized {
    if (collectingHbs.get()) {
      val key = CompoundKey(hbMsg.hb.hbType, hbMsg.hb.id)
      hbs += (key -> hbMsg)
      HbsCollector.logger.debug(s"HB received from a ${key.hbProducerType} with ID ${key.id}")
    }
  }
}

/** Companion object */
object HbsCollector {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(HbsCollector.getClass)
}