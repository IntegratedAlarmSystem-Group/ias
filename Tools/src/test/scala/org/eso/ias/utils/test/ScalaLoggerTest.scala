package org.eso.ias.utils.test

import ch.qos.logback.classic.Level
import org.eso.ias.logging.IASLogger

class ScalaLoggerTest {

  /**
    * Produces the logs
    *
    * @param alsoDebug if true emits DEBUG logs too
    */
  def produceLogs(alsoDebug: Boolean) {
    ScalaLoggerTest.logger.info("Generating logs")
    
    var iteration = 0L
    
    while (true) {
      iteration = iteration+1
      ScalaLoggerTest.logger.info(ScalaLoggerTest.logPrefix+iteration.toString())
      if (alsoDebug)ScalaLoggerTest.logger.debug(ScalaLoggerTest.debugLogPrefix+iteration.toString())
    }
  }
  
}

object ScalaLoggerTest extends App {
    
  val logger = IASLogger.getLogger(ScalaLoggerTest.getClass)
  IASLogger.setRootLogLevel(Level.DEBUG)
  
  val logPrefix = "Prefix log to messages to write something and make a log message longer "

  val debugLogPrefix = "Prefix DEBUG log to messages to write something and make a log message longer "
  
  val v = new ScalaLoggerTest()
  v.produceLogs(true)
    
  
}