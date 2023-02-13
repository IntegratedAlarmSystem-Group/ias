package org.eso.ias.kafkaneo.consumer

import com.typesafe.scalalogging.Logger
import org.eso.ias.kafkaneo
import org.eso.ias.kafkaneo.{ConsumersStatistics, IasTopic}
import org.eso.ias.logging.IASLogger
import org.slf4j.event.Level

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.{Collections, Timer, TimerTask}

/**
 * Build and publish the statistics for the consumers in the form of
 * log messages.
 *
 * The ConsumersStatistics collects the number of events read from the topics and periodically
 * publish log messages with the statistics.
 * The period to collect data and publish statistics and the log level to use are customizable.
 *
 * [[ConsumersStatistics.start]] must be invoked to activate the timer task that periodically
 * publishes the logs. [[ConsumersStatistics.stop)]] must be invoked to stop publishing
 * the statistics. Once stopped the ConsumersStatistics cannot be started again.
 *
 * In case of overflow incrementing the number of the events read from the topics,
 * the numbers are reset to 0.
 * For synchronization, ConsumersStatistics uses [[AtomicLong]] whose values are reset to 0 when
 * there is an overflow. The operation of (increase -> checkOverflow -> reset) is not atomic
 * so it could happen that the statistics are not reporting useful values in that case but such event
 * is so rare that it is not worst to add more complexity.
 *
 * @param id The id for the consumer
 * @param timerPeriod the number of minutes to push statistics
 * @param logLevel: the level of the logs published for the statistics
 */
class ConsumersStatistics(val id: String, val timerPeriod: Int=10, val logLevel: Level=Level.INFO)
extends TimerTask {
  require(Option(id).map(!_.isBlank).getOrElse(false))
  require(timerPeriod>1)

  /** The timer to periodically publish the logs of statistics */
  val timer: Timer = new Timer(s"StatisticsTimer_$id", true)

  /** Signal if the timer has been started */
  val started: AtomicBoolean = new AtomicBoolean(false)

  /** Signal if the timer has been stopped */
  val stopped: AtomicBoolean = new AtomicBoolean(false)

  /**
   * The numbers collected for each topic
   *
   * @param totRecordsInPeriod the number of records processed in the ongoing period
   *                           It is reset when the period elapses
   * @param totMsgs total number of messages (each message can contains more records)
   * @param totRecords total number of events
   */
  class Statistics() {
    val totRecordsInPeriod = new  AtomicLong(0)
    val totMsgs = new  AtomicLong(0)
    val totRecords = new  AtomicLong(0)
  }

  /** The statistics for each topic */
  val stats: Map[IasTopic, Statistics] =  {
    val m = scala.collection.mutable.Map[IasTopic, Statistics]()
    IasTopic.values.foreach(t => m(t) = new Statistics())
    m.toMap
  }

  /**
   * A new message has been received with the passed number of records.
   *
   * This function shall be invoked for every new message read from a topic (i.e. when the kafka
   * poll exits)
   *
   * @param topic the topic from where the records have been consumed
   * @param numOfRecords the number of records in the message
   */
  def newRecordsReceived(topic: IasTopic, numOfRecords: Int): Unit = {
    val newValue = stats(topic).totMsgs.incrementAndGet()
    if (newValue<0)  then stats(topic).totMsgs.set(0)

    val recValue = stats(topic).totRecords.addAndGet(numOfRecords)
    if (recValue<0)  then stats(topic).totRecords.set(0)

    val periodValue = stats(topic).totRecordsInPeriod.addAndGet(numOfRecords)
    if (periodValue<0) stats(topic).totRecordsInPeriod.set(0) // Very unlikely
  }

  /** Start the timer task to publish the logs with statistics */
  def start(): Unit = {
    (started.get(), stopped.get()) match {
      case (false, false) =>
        timer.schedule(this, 60*1000*timerPeriod, 60*1000*timerPeriod)
        ConsumersStatistics.logger.debug("Periodic task to publish statistics started")
        started.set(true)
      case (true, false) => ConsumersStatistics.logger.warn("Periodic task to publish statistics already started")
      case (true, true) => ConsumersStatistics.logger.error("Periodic task to publish statistics cannot be restarted")
      case (false, true) => throw new IllegalStateException("ConsumerStatstics invalid state: NOT started but stopped!!")
    }
  }

  /** Stop the timer task to publish statistics  */
  def stop(): Unit = {
    (started.get(), stopped.get()) match {
      case (true, false) =>
        stopped.set(true)
        timer.cancel()
        ConsumersStatistics.logger.debug("Periodic task to publish statistics stopped")
      case (true, true) => ConsumersStatistics.logger.warn("Periodic task to publish statistics already stopped")
      case (false, false) => ConsumersStatistics.logger.error("Periodic task cannot be stopped: never started")
      case (false, true) => throw new IllegalStateException("ConsumerStatstics invalid state: NOT started but stopped!!")
    }
  }

  /** The timer task that publishes the statistics */
  def run(): Unit = {
    def log(logMsg: String): Unit = {
      (logLevel) match {
        case Level.INFO => ConsumersStatistics.logger.info(logMsg)
        case Level.WARN => ConsumersStatistics.logger.warn(logMsg)
        case Level.DEBUG => ConsumersStatistics.logger.debug(logMsg)
        case Level.ERROR => ConsumersStatistics.logger.error(logMsg)
        case Level.TRACE => ConsumersStatistics.logger.trace(logMsg)
      }
    }


    stats.foreach {
      case (topic, values) => {
        val msg = s"Stats. for consumer $id on topic $topic: ${values.totMsg.get()} msgs processed, ${values.totRecords.get()} records (${values.totRecords.get()/timerPeriod} rec/min)"
        log(msg)
      }
    }
  }
}

object ConsumersStatistics {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(ConsumersStatistics.getClass)
}
