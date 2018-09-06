package org.eso.ias.dasu.executorthread

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.lang.Runtime
import scala.util.control.NonFatal
import scala.util.Try

/** The scheduled executor for the transfer functions of the DASU. 
 *  
 *  @constructor Builds the scheduled executor
 *  @param dasuID the identifier of the DASU
 *  @param coreSize the size of the core of the scheduled executor
 *  */
class ScheduledExecutor(dasuId: String, coreSize: Int) 
extends ScheduledThreadPoolExecutor(coreSize, new DasuThreadFactory(dasuId)) {
  
  
  /**
   * Build a scheduled executor taking the core size from java properties or using the default
   */
  def this(dasuId: String) {
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
  lazy val CorePoolSizeDefaultValue = Runtime.getRuntime().availableProcessors()/2
  
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