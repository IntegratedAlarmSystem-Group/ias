package org.eso.ias.dasu

import org.eso.ias.logging.IASLogger
import org.eso.ias.cdb.pojos.DasuDao
import scala.collection.JavaConverters
import org.eso.ias.cdb.pojos.IasioDao
import scala.util.Try
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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
  require(Option(dasuId).isDefined && !dasuId.isEmpty(),"Invalid DASU ID")

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  logger.debug("Building the statistics collector for DASU [{}]",dasuId)

  /** The number of iterations executed so far */
  val iterationsRun = new AtomicLong(0L)

  /** The average execution time */
  val avgExecutionTime = new AtomicReference[Double](0.0)

  /** The time interval to publish statistics in msecs */
  val statsTimeInterval = TimeUnit.MILLISECONDS.convert(DasuStatistics.StatisticsTimeInterval,TimeUnit.MINUTES)
  if (statsTimeInterval>0) {
    logger.info(f"DASU [$dasuId%s] will generate stats every ${DasuStatistics.StatisticsTimeInterval} minutes")
  } else {
    logger.warn("Generation of stats for DASU [{}] disabled",dasuId)
  }

  logger.info("DASU [{}] statistics collector built",dasuId)

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
  override def logStats() = {
    val numOfIterationsRun = {
      val last=iterationsRun.incrementAndGet()
      if (last<0) {
        avgExecutionTime.set(0.0)
        iterationsRun.set(1)
      }
      iterationsRun.get
    }
    logger.info(f"DASU [$dasuId%s]: avg time of calculation of the output ${avgExecutionTime.get()}%.2f ms, output calculated ${iterationsRun.get}%d times)")
  }

  /**
   * Endorse the passed execution time to generate statistics.
   *
   * @param lastExecTime the execution time (msec) taken to update the output in the last run
   */
  def updateStats(lastExecTime: Long) {
    require(Option(lastExecTime).isDefined && lastExecTime>=0,"Invalid execution time")


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
