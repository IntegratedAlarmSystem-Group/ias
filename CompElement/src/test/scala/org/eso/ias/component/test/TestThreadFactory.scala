package org.eso.ias.component.test

import java.util.concurrent.ThreadFactory

/**
   * ThreadFactory for testing.
   * 
   * It keeps track of all the running threads to check if they
   * terminated
   */
  class TestThreadFactory() extends ThreadFactory {
    private var threads: List[Thread] = Nil
    
    private var count: Int = 0
     def newThread(r: Runnable): Thread = {
       val t: Thread = new Thread(r, "TestTransferFunctionSetting thread #"+count);
       t.setDaemon(true)
       threads = t::threads
       t
     }
    
    /**
     * The total number of threads instantiated by this 
     * factory
     */
    def instantiatedThreads = threads.size
    
    /**
     * @return The number of alive threads
     */
    def numberOfAliveThreads(): Int = {
      threads.filter(t => t.isAlive()).size
    }
  }