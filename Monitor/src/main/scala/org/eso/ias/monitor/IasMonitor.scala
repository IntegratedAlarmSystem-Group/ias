package org.eso.ias.monitor

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, CommandLineParser, DefaultParser, HelpFormatter, Options}
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbFiles, CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.LogLevelDao
import org.eso.ias.cdb.rdb.RdbReader
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Identifier, IdentifierType}

import scala.util.{Failure, Success, Try}

/**
  * Monitor the stat of the IAS and sends alarms
  *
  * In this version, alarms are pushed in the core topic and willl  be read
  * from there by the web server.
  * TODO: send alarms (al least some of them) to the web server even wehn
  *       kafka is down
  */
class IasMonitor {
}

object IasMonitor {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(IasMonitor.getClass)

  /** Build the usage message */
  val cmdLineSyntax: String = "Monitor Monitor-ID [-h|--help] [-j|-jcdb JSON-CDB-PATH] [-x|--logLevel log level]"

  /**
    * Parse the command line.
    *
    * If help is requested, prints the message and exits.
    *
    * @param args The params read from the command line
    * @return a tuple with the Id of the monitor, the path of the cdb and the log level dao
    */
  def parseCommandLine(args: Array[String]): (Option[String],  Option[String], Option[LogLevelDao]) = {
    val options: Options = new Options
    options.addOption("h", "help",false,"Print help and exit")
    options.addOption("j", "jcdb", true, "Use the JSON Cdb at the passed path")
    options.addOption("x", "logLevel", true, "Set the log level (TRACE, DEBUG, INFO, WARN, ERROR)")

    val parser: CommandLineParser = new DefaultParser
    val cmdLineParseAction = Try(parser.parse(options,args))
    if (cmdLineParseAction.isFailure) {
      val e = cmdLineParseAction.asInstanceOf[Failure[Exception]].exception
      println(e + "\n")
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(-1)
    }

    val cmdLine = cmdLineParseAction.asInstanceOf[Success[CommandLine]].value
    val help = cmdLine.hasOption('h')
    val jcdb = Option(cmdLine.getOptionValue('j'))

    val logLvl: Option[LogLevelDao] = {
      val t = Try(Option(cmdLine.getOptionValue('x')).map(level => LogLevelDao.valueOf(level)))
      t match {
        case Success(opt) => opt
        case Failure(f) =>
          println("Unrecognized log level")
          new HelpFormatter().printHelp(cmdLineSyntax, options)
          System.exit(-1)
          None
      }
    }

    val remaingArgs = cmdLine.getArgList

    val monitorId = if (remaingArgs.isEmpty) None else Some(remaingArgs.get(0))

    if (!help && monitorId.isEmpty) {
      println("Missing Monitor ID")
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(-1)
    }
    if (help) {
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(0)
    }

    val ret = (monitorId, jcdb, logLvl)
    IasMonitor.logger.info("Params from command line: jcdb={}, logLevel={} supervisor ID={}",
      ret._2.getOrElse("Undefined"),
      ret._3.getOrElse("Undefined"),
      ret._1.getOrElse("Undefined"))
    ret

  }

  /**
    * Entry point of the monitor tool
    *
    * @param args command line args
    */
  def main(args: Array[String]): Unit = {
    val parsedArgs = parseCommandLine(args)
    require(parsedArgs._1.nonEmpty, "Missing identifier in command line")

    val monitorId = parsedArgs._1.get

    val reader: CdbReader = {
      if (parsedArgs._2.isDefined) {
        val jsonCdbPath = parsedArgs._2.get
        logger.info("Using JSON CDB @ {}",jsonCdbPath)
        val cdbFiles: CdbFiles = new CdbJsonFiles(jsonCdbPath)
        new JsonReader(cdbFiles)
      } else {
        new RdbReader()
      }
    }

    /**
      *  Get the configuration of the IAS from the CDB
      */
    val (refreshRate, hbFrequency, logLvlvFromCdb, kafkaBrokers) = {

      val iasDaoOpt = reader.getIas
      require(iasDaoOpt.isPresent, "Cannot read IAS config from CDB")

      val refRate: Int = iasDaoOpt.get().getRefreshRate
      val hbRate: Int = iasDaoOpt.get().getHbFrequency
      val logLvl: LogLevelDao = iasDaoOpt.get.getLogLevel
      val brokers: String = iasDaoOpt.get.getBsdbUrl


      (refRate, hbRate, logLvl, brokers)
    }

    val supervisorIds = {
      val idsOpt = reader.getSupervisorIds

    }
    reader.shutdown()

     // The identifier of the monitor
    val identifier = new Identifier(monitorId, IdentifierType.CLIENT, None)
  }
}
