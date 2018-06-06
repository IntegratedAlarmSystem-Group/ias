package org.eso.ias.logging;

import com.typesafe.scalalogging.Logger

import org.slf4j.LoggerFactory
import org.slf4j.ILoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter

/**
 * IASLogger is the binding to the logging mechanism for the IAS.
 * 
 * The purpose of IASLogger is to have a centralized initialization
 * of the logger for all IAS applications so that it will be 
 * possible to change the logging by changing this class
 * but transparently to user applications.
 * 
 * The user shall get a logger with one of the factory methods.
 * 
 * @author acaproni
 * @since Oct 2016
 */
object IASLogger {
  
  /**
   * The global logger.
   */
  private val globalLogger = Logger("IAS-Global-Logger")
  
  //printLoggerStatus()
  /**
   * @return A logger with the given name
   */
  def getLogger(name: String) = Logger(name)
  
  def getLogger(c: java.lang.Class[_]) = Logger(c)
  
  /**
   * Print the status of the logger in the stdout
   * for debugging purposes
   * 
   * logback configuration file can be tweaked to automatically
   * print debug information even if there is no error by setting 
   * the debug attribute of the configuration tag to true 
   */
  def printLoggerStatus() {
    val iLoggerFactory: ILoggerFactory = LoggerFactory.getILoggerFactory()
    val lc: LoggerContext = iLoggerFactory match {
      case temp: LoggerContext => temp
      case _ => throw new ClassCastException
    }
    StatusPrinter.print(lc);
  }
}
