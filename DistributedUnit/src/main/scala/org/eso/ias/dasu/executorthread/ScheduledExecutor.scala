package org.eso.ias.dasu.executorthread

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.lang.Runtime
import scala.util.control.NonFatal
import scala.util.Try

import org.eso.ias.logging.IASLogger

/** The scheduled executor for the transfer functions of the DASU. 
 *  
 *  @constructor Builds the scheduled executor
 *  @param dasuID the identifier of the DASU
 *  @param coreSize the size of the core of the scheduled executor
 *  */
class ScheduledExecutor(dasuId: String, coreSize: Int) 
extends ScheduledThreadPoolExecutor(coreSize, new DasuThreadFactory(dasuId)) {

  ScheduledExecutor.logger.debug("Executor for DASU [{}]:  core size={}, pool size={}, queue size={}, active count={})",
            dasuId,
            getCorePoolSize(),
            getPoolSize(),
            getQueue.size(),
            getActiveCount())
  
  
  /**
   * Build a scheduled executor taking the core size from java properties or using the default
   */
  def this(dasuId: String) = {
    this(dasuId,ScheduledExecutor.getCoreSize())
  }
}

object ScheduledExecutor {
  
  /**
   * The name of the property to set the number of cores in the thread executor
   */
  val CoreSizePropName = "ias.dasu.threadpoolcoresize"
  
  /**
   * The default size of the pool
   */
  lazy val CorePoolSizeDefaultValue = {
      val defaultByProcNum = Runtime.getRuntime().availableProcessors()/2
      if (defaultByProcNum > 0) defaultByProcNum else 1
    }

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /**
   * Get the size of the core from the java property or from
   * the number of available CPUs
   */
  def getCoreSize(): Int = {
    // Check if the java property has been set in the environment
    import java.util.Properties
    val props = System.getProperties()
    val coreFromProps = props.getProperty(CoreSizePropName,"")
    Try(coreFromProps.toInt).getOrElse(CorePoolSizeDefaultValue)
  }
  
}