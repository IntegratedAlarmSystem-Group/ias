package org.eso.ias.asce

import java.util.concurrent.ThreadFactory

/**
 * The thread factory for the threads spawn by the {@link ComputingElement}
 * instrumented with its runningID.
 */
class CompEleThreadFactory(val runningID: String) extends ThreadFactory 
{
  override def newThread(r: Runnable): Thread = {
     val t = new Thread(r,runningID);
     t.setDaemon(true)
     t
   }
}