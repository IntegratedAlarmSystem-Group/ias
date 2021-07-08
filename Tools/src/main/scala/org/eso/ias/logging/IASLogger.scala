package org.eso.ias.logging;

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.slf4j.ILoggerFactory
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.classic.{Logger => LogBackLogger}

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
  def printLoggerStatus(): Unit = {
    val iLoggerFactory: ILoggerFactory = LoggerFactory.getILoggerFactory()
    val lc: LoggerContext = iLoggerFactory match {
      case temp: LoggerContext => temp
      case _ => throw new ClassCastException
    }
    StatusPrinter.print(lc);
  }

  /**
    * Set the log level of the root to the passed level/
    *
    * This code is dependent of logback because slf4j does not
    * offer any API to set the log level.
    *
    * @param level
    */
  def setRootLogLevel(level: Level): Unit = {
   require(Option(level).isDefined)
   val loggerFactory =  LoggerFactory.getILoggerFactory
   val rootLogger: LogBackLogger = loggerFactory.getLogger("org.eso.ias").asInstanceOf[LogBackLogger]
   rootLogger.setLevel(level)
  }

  /**
    * Set the log level depending if it is passed in the command line,
    * the IAS configuration or the configuration of the tool like the Supervisor)
    *
    * If none of the level is defined, no log level is set.
    *
    * The precedence is as follow:
    * 1 command line
    * 2 tool
    * 3 IAS
    *
    * @param commandLineLevel the level read from the command line, if present
    * @param iasConfigLevel the level read from the IAS configuration in the CDB, if present
    * @param toolLevel the level read from the tool configuration in the CDB, if present
    * @return the log level that has been set; undefined otherwise
    */
  def setLogLevel(commandLineLevel: Option[Level], iasConfigLevel: Option[Level], toolLevel: Option[Level]): Option[Level] = {
    (commandLineLevel, iasConfigLevel, toolLevel) match {
      case (Some(level), _, _ ) => setRootLogLevel(level) // Command line
        Some(level)
      case (None, _ , Some(level)) => setRootLogLevel(level) // Tool
        Some(level)
      case (None, Some(level), None) => setRootLogLevel(level) // IAS
        Some(level)
      case (None, None, None) => None
    }
  }

  /**
    * Set the log level depending if it is passed in the command line,
    * the IAS configuration or the configuration of the tool like the Supervisor)
    *
    * This is a helper method for java
    *
    * @param commandLineLevel the level read from the command line (can be null)
    * @param iasConfigLevel the level read from the IAS configuration in the CDB  (can be null)
    * @param toolLevel the level read from the tool configuration in the CDB  (can be null)
    * @return the log level that has been set; null otherwise
    */
  def setLogLevel(commandLineLevel: Level, iasConfigLevel: Level, toolLevel: Level): Level = {
    setLogLevel(Option(commandLineLevel), Option(iasConfigLevel), Option(toolLevel)).getOrElse(null)
  }
}
