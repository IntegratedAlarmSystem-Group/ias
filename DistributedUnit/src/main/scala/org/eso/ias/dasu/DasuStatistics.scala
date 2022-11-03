package org.eso.ias.dasu

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import org.eso.ias.logging.IASLogger

import scala.util.Try

/**
 * StasCollector generates and publish statistics of the generation of the output
 * by the DASU.
 * 
 * The method updateStats is expected to be calles after each generation of the output by the DASU.
 * 
 * @author acaproni
 * 
 * @param dasuId: the ID of the DASU
 */
class DasuStatistics(
    val dasuId: String) extends StatsCollectorBase(dasuId,DasuStatistics.StatisticsTimeInterval) {
  require(Option(dasuId).isDefined && !dasuId.isEmpty,"Invalid DASU ID")

  DasuStatistics.logger.debug("Building the statistics collector for DASU [{}]",dasuId)

  /** The number of iterations executed so far */
  val iterationsRun = new AtomicLong(0L)

  /** The average execution time */
  val avgExecutionTime = new AtomicReference[Double](0.0)

  /** The execution time spent by the DASU to produce the last output */
  val lastExecutionTime = new AtomicLong(0)

  /** The max time spent by the DASU to produce the output */
  val maxExecutionTime  = new AtomicLong(0)

  /** The time interval to publish statistics in msecs */
  val statsTimeInterval: Long = TimeUnit.MILLISECONDS.convert(DasuStatistics.StatisticsTimeInterval,TimeUnit.MINUTES)
  if (statsTimeInterval>0) {
    DasuStatistics.logger.info(f"DASU [$dasuId%s] will generate stats every ${DasuStatistics.StatisticsTimeInterval} minutes")
  } else {
    DasuStatistics.logger.warn("Generation of stats for DASU [{}] disabled",dasuId)
  }

  DasuStatistics.logger.info("DASU [{}] statistics collector built",dasuId)

  /**
   * Calculate the (aprox) mean of last executions.
   *
   * We approximate the mean but do not have to stare the samples.
   *
   * I took the algorithm from
   * [[https://math.stackexchange.com/questions/106700/incremental-averageing math.stackexchange]]
   *
   * @param actMean the actual mean
   * @param sample the new sample
   * @param iter the number of iterations
   */
  private def mean(actMean: Double, sample: Long, iter: Long): Double = { actMean +(sample-actMean)/iter }

  /**
    * The method to log statistics, called at regular time intervals
    */
  override def logStats(): Unit = {
    val numOfIterationsRun = {
      val last=iterationsRun.incrementAndGet()
      if (last<0) {
        avgExecutionTime.set(0.0)
        iterationsRun.set(1)
      }
      iterationsRun.get
    }
    DasuStatistics.logger.info(
      f"DASU [$dasuId%s]: last prod. time of output=${lastExecutionTime.get}%d ms, max time=${maxExecutionTime.get}%d ms (avg  ${avgExecutionTime.get()}%.2f ms); output calculated ${iterationsRun.get}%d times)")

  }

  /**
   * Endorse the passed execution time to generate statistics.
   *
   * @param lastExecTime the execution time (msec) taken to update the output in the last run
   */
  def updateStats(lastExecTime: Long): Unit = {
    require(Option(lastExecTime).isDefined && lastExecTime>=0,"Invalid execution time")

    lastExecutionTime.set(lastExecTime)
    if (lastExecTime>maxExecutionTime.get) maxExecutionTime.set(lastExecTime)

    val last=iterationsRun.incrementAndGet()
    if (last<0) {
      avgExecutionTime.set(0.0)
      iterationsRun.set(0)
    } else {
      avgExecutionTime.set(mean(avgExecutionTime.get,lastExecTime,iterationsRun.get))
    }
  }
}

/** Companion object with definitions of constants*/
object DasuStatistics {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** The time interval to log statistics (minutes) */
  val DeafaultStatisticsTimeInterval = 10
  
  /** The name of the java property to set the statistics generation time interval */
  val StatisticsTimeIntervalPropName = "ias.dasu.stats.timeinterval"
  
  /** The actual time interval to log statistics (minutes) */
  val StatisticsTimeInterval: Int = {
    val prop = Option(System.getProperties.getProperty(StatisticsTimeIntervalPropName))
    prop.map(s => Try(s.toInt).getOrElse(DeafaultStatisticsTimeInterval)).getOrElse(DeafaultStatisticsTimeInterval).abs
  }
}
