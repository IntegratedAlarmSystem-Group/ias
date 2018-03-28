package org.eso.ias.dasu.executorthread

import java.util.concurrent.ThreadFactory
import org.eso.ias.utils.ISO8601Helper

/** The thread factory for the DASU */
class DasuThreadFactory(private val dasuId: String) extends ThreadFactory {
  require(Option(dasuId).isDefined && !dasuId.isEmpty(),"Invalid ID of the DASU")
  
  /** The thread group */
  val threadGroup= new ThreadGroup("DausThreadGroup")
  
  /** Generate a new thread for the DASU */
  override def newThread(r: Runnable): Thread = {
     val ret = new Thread(threadGroup,r,"DasuThread-"+dasuId+"@"+ISO8601Helper.now())
     ret.setDaemon(true)
     ret
   }
  
}
