package org.eso.ias.dasu

import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import java.util.concurrent.atomic.AtomicReference
import org.eso.ias.logging.IASLogger

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

  /** The future to cancel logging of statistics */
  private var future = new AtomicReference[ScheduledFuture[?]]()

  /**
    * The method to log statistics, called at regular time intervals
    */
  def logStats(): Unit

  /** Start logging statistics */
  def start() = synchronized {
    Option(future.get()) match {
      case Some(f) => StatsCollectorBase.logger.warn("Stats generation for " + id + " already started")
      case None => future.set(scheduler.scheduleAtFixedRate(() =>
        logStats(),
        timeInterval,
        timeInterval,
        TimeUnit.MINUTES))
    }
    StatsCollectorBase.logger.info("Stats generation for " + id + " started: will log stats every " + timeInterval + " minutes")
  }

  /** Stop loging statistics */
  def cleanUp() = synchronized {
    Option(future.getAndSet(null)).foreach(f => {
      f.cancel(false)
      StatsCollectorBase.logger.info("Stats generation for +"+id+" closed")
    })
  }
}

object StatsCollectorBase {
  /** The logger */
  val logger = IASLogger.getLogger(StatsCollectorBase.getClass)
}
