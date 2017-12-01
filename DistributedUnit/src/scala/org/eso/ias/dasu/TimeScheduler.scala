package org.eso.ias.dasu

import org.ias.prototype.logging.IASLogger
import org.eso.ias.cdb.pojos.DasuDao
import scala.collection.JavaConverters
import org.eso.ias.cdb.pojos.IasioDao
import scala.util.Try

/**
 * The TimeScheduler helps setting the time interval to generate the 
 * output when no new inputs arrive.
 * 
 * Note that this time has nothing to do to the update when new inputs arrive: 
 * in that case the update of the output happens immediately
 * 
 * After updating the output, the DASU does nothing until a new set of inputs 
 * is submitted. The output must be regenerated even if there are no new inputs
 * at least to assess its validity.
 * 
 * At first approximation, the refresh must be scheduled at the minimum
 * refresh rate of all the inputs to be able to catch the case of
 * that input becoming invalid.
 * 
 * The refresh rate should also take into account the execution time of 
 * the update of the output by all the ASCEs.
 * In first approximation given
 * - m = min refresh rate (minRefreshRate) between all possible inputs
 *       and the output of the ASCE
 * - e = average execution time of the TF
 * - a - a margin to anticipate the generation of the output to reduce
 *       the risk to be too late
 * the next refresh must be scheduled in m-e-a msecs.
 * 
 * However if m-e is too short then the update is executed too often wasting
 * too much CPU. So m-e must be limited to give other processes the possibility
 * to get CPU time. This will be important when many DASUs will run in the 
 * same supervisor. 
 * 
 * The getNextRefreshTime is supposed to be called after each generation of the output
 * to schedule the next execution when no new inputs arrive. 
 * This seems also the best place to log some statistics
 * 
 * @author acaproni
 * 
 * @param dasuDao the configuration of the DASU
 */
class TimeScheduler(dasuDao: DasuDao) {
  require(Option(dasuDao).isDefined,"Invalid DASU configuration")
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /**
   * The identifier of the DASU for log messages
   */
  private val dasuId = dasuDao.getId
  
  /** The refresh rate of the output */
  private val outputRefreshTime = dasuDao.getOutput.getRefreshRate
  
  /** The shortest refresh rate between all the possible inputs */
  private val minRefreshRateOfInputs: Int = {
    val asces = JavaConverters.asScalaSet(dasuDao.getAsces).toSet
    val inputs = asces.flatMap(asce => JavaConverters.collectionAsScalaIterable(asce.getInputs))
    def min(a: IasioDao, b: IasioDao) = if (a.getRefreshRate < b.getRefreshRate) a else b
    inputs.reduceLeft(min).getRefreshRate
  }
  
  /**
   * The minimum allowed refresh rate is given by [[TimeScheduler.DefaultMinAllowedRefreshRate]]
   * or passed through a java property
   */
  val minAllowedRefreshRate = {
    val prop = Option(System.getProperties.getProperty(TimeScheduler.MinAllowedRefreshRatePropName))
    prop.map(s => Try(s.toInt).getOrElse(TimeScheduler.DefaultMinAllowedRefreshRate)).getOrElse(TimeScheduler.DefaultMinAllowedRefreshRate).abs
  }
  
  val margin = {
    val prop = Option(System.getProperties.getProperty(TimeScheduler.MarginPropName))
    prop.map(s => Try(s.toInt).getOrElse(TimeScheduler.DefaultMargin)).getOrElse(TimeScheduler.DefaultMargin).abs
  }
  
  /** 
   *  The minimum refresh rate is the minimum between the 
   *  refresh rate of all the possible inputs and the 
   *  refresh rate of the output but cannot be greater
   *  the the max allowed refresh rate
   */
  val minRefreshRate = outputRefreshTime.min(minRefreshRateOfInputs).max(minAllowedRefreshRate)
  
  /** The refresh rate taking into account the  margin */
  val normalizedRefreshRate = (minRefreshRate-margin).max(minAllowedRefreshRate)
  
  
  /** The number of iterations executed so far */
  private var iterationsRun = 0L
  
  /** The average execution time */
  private var avgExecutionTime = 0.0
  
  /** The time of the last publication of the statistics */
  private var lastStatsPubicationTime = System.currentTimeMillis()
  
  /** The time interval to publish statistics in msecs */ 
  val statsTimeInterval = Dasu.StatisticsTimeInterval*1000
  if (statsTimeInterval>0) {
    logger.info(f"DASU [$dasuId%s] will generate stats for every ${Dasu.StatisticsTimeInterval} minutes")
  } else {
    logger.warn("Will NOT generate stats for DASU [{}]",dasuId)
  }
  
  logger.info(f"DASU [$dasuId%s]: automatic refresh of output every ms ${minRefreshRate-margin} ")
  
  /**
   * Calculate the aprox mean of last executions.
   * 
   * We approximate the mean but do not have to stare the samples.
   * 
   * I took the algorithm from 
   * [[https://math.stackexchange.com/questions/106700/incremental-averageing math.stackexchange]]
   * 
   * @param actMean the actual mean
   * @param sample the new sample
   * @param the number of iterations
   */
  private def mean(actMean: Double, sample: Long, iter: Long): Double = { actMean +(sample-actMean)/iter }
  
  /**
   * Calculate the time interval in msec to update the output.
   * 
   * @param lastExecTime the execution time (msec) taken to update the output in the last run
   * @return the time (msec) to start calculating the output
   */
  def getNextRefreshTime(lastExecTime: Long): Int = {
    require(Option(lastExecTime).isDefined && lastExecTime>0,"Invalid execution time")
    
    iterationsRun =  iterationsRun match {
      case Long.MaxValue => avgExecutionTime=0.0; 0L
      case n => n+1
    }
    
    avgExecutionTime=mean(avgExecutionTime,lastExecTime,iterationsRun)
    
    // publish a log of stats if the time interval elapsed
    if (System.currentTimeMillis()>lastStatsPubicationTime+statsTimeInterval) {
      logger.info(f"DASU [$dasuId%s]: average time of calculation of the output $avgExecutionTime%.2f ms (last took $lastExecTime%d ms), output calculated $iterationsRun%d times)")      
    }
    
    val executeIn = normalizedRefreshRate-avgExecutionTime.toInt
    
    if (lastExecTime>=executeIn) {
      logger.warn(f"DASU [$dasuId%s] took too long ($lastExecTime%d) to produce the output at the requested rate of ${minRefreshRate-margin}%d")
    }
    
    if (executeIn<minAllowedRefreshRate) {
      logger.warn(f"DASU [$dasuId%s] time interval for next update increased to the min allowed refresh rate $minAllowedRefreshRate")
    }
    
    normalizedRefreshRate-avgExecutionTime.toInt.max(minAllowedRefreshRate)
  }
}

/** Companion object with definitions of constants*/
object TimeScheduler {
  
  /** The minimum possible refresh rate */
  val DefaultMinAllowedRefreshRate = 500
  
  /** The name of the java property to set the minimum allowed refresh rate */
  val MinAllowedRefreshRatePropName = "ias.dasu.min.allowed.output.refreshrate"
  
  /** The default margin to anticipate the generation of the output  */
  val DefaultMargin = 100
  
  /** The name of the java property to set the margin */
  val MarginPropName = "ias.dasu.min.output.generation.margin"
}