package org.eso.ias.sink

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong


class ProcessorThreadFactory(val processorId: String) extends ThreadFactory {

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
    val ret = new Thread(runnable,s"$processorId-$actual")
    ret.setDaemon(true)
    ret
  }
}
