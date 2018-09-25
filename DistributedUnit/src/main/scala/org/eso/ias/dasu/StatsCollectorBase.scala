package org.eso.ias.dasu

import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import java.util.concurrent.atomic.AtomicReference

import org.eso.ias.supervisor.StatsLogger

/**
  * Base class for generating statistics at definite time intervals
  *
  * @param id The identifer of the generator of istatistics
  * @param timeInterval The time interval to generate statistics (minutes)
  */
abstract class StatsCollectorBase(id: String, timeInterval: Long) {
  require(Option(id).isDefined && id.nonEmpty,"Invalid id")
  require(timeInterval>0,"Invalid negative time interval "+timeInterval)

  /** The scheduler to log statistics */
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  /** The scheduler to log statistics */
  private val scheduer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  /** Th efuture to cancel logging of statistics */
  private var future = new AtomicReference[ScheduledFuture[_]]()

  /**
    * The method to log statistics, called at regular time intervals
    */
  def logStats()

  /** Start logging statistics */
  def start() = synchronized {
    Option(future.get()) match {
      case Some(f) => StatsLogger.logger.warn("StatsCollector "+id+" already started")
      case None => future.set(scheduer.scheduleAtFixedRate( () =>
        logStats(),
        timeInterval,
        timeInterval,
        TimeUnit.MINUTES))
    }
    StatsLogger.logger.info("StatsLogger "+id+" started: will log stats every "+timeInterval+" minutes")
  }

  /** Stop loging statistics */
  def cleanUp() = synchronized {
    Option(future.getAndSet(null)).foreach(f => {
      f.cancel(false)
      StatsLogger.logger.info("StatsLogger +"+id+" closed")
    })
  }

}
