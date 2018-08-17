package org.ias.logging.test

import org.eso.ias.logging.IASLogger

class ScalaLoggerTest {
  
  def produceLogs() {
    ScalaLoggerTest.logger.info("Generating logs")
    
    var iteration = 0L
    
    while (true) {
      iteration = iteration+1
      ScalaLoggerTest.logger.info(ScalaLoggerTest.logPrefix+iteration.toString())
    }
  }
  
}

object ScalaLoggerTest extends App {
    
  val logger = IASLogger.getLogger(ScalaLoggerTest.getClass)
  
  val logPrefix = "Prefix log to messages to write something and make a log message longer "
  
  val v = new ScalaLoggerTest()
  v.produceLogs()
    
  
}