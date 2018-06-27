package org.eso.ias.sink

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

import org.eso.ias.logging.IASLogger


class ProcessorThreadFactory(val processorId: String) extends ThreadFactory {

  /** The logger */
  private val logger = IASLogger.getLogger(classOf[ProcessorThreadFactory])

  /**
    * Count the number of created threads
    */
  val count = new AtomicLong(0)

  /**
    * Create a new thread
    *
    * @param runnable The runnable
    * @return the thread
    */
  override def newThread(runnable: Runnable): Thread = {
    require(Option(runnable).isDefined)
    val actual = count.incrementAndGet()
    val threadId = s"$processorId-$actual"
    val ret = new Thread(runnable,threadId)
    ret.setDaemon(true)
    logger.debug("New thread {} created",threadId)
    ret
  }
}
