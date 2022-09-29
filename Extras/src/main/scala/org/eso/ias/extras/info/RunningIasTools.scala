package org.eso.ias.extras.info

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, CommandLineParser, DefaultParser, HelpFormatter, Options}
import org.eso.ias.cdb.pojos.LogLevelDao
import org.eso.ias.heartbeat.HeartbeatProducerType
import org.eso.ias.heartbeat.report.HbsCollector
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.logging.IASLogger

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
 * Get the HBs and prints in the stdout the list of the IAS running
 *
 * @param params the parameters set in the command line
 */
class RunningIasTools(params: RunningIasTools.CmdLineParams) {
  // The ID to connect to kafka
  val id = s"RunningIasTools-${System.currentTimeMillis()}"

  /** The flag to write in verbose */
  val verboseOn: Boolean = params.verbose

  // The collector (it will not remove older HBs)
  val collector : HbsCollector = new HbsCollector(params.brokers,id)

  val collectingTime: Long = params.timeout
  require(collectingTime>=RunningIasTools.MIN_COLLECTING_TIME,
    s"Timeout shall be greater than ${RunningIasTools.MIN_COLLECTING_TIME}")

  val cleanedUp: AtomicBoolean = new AtomicBoolean(false) // Avoid cleaning up twice
  val shutDownThread: Thread =addsShutDownHook()

  /** Adds a shutdown hook to cleanup resources before exiting */
  private def addsShutDownHook(): Thread = {
    val t = new Thread() {
      override def run(): Unit = {
        RunningIasTools.logger.debug("Shutdown hook is closing the collector")
        shutdown()
      }
    }
    Runtime.getRuntime.addShutdownHook(t)
    t
  }

  /** Setup */
  def setup(): Unit = collector.setup()

  def shutdown(): Unit = {
    val alreadyShutdown = cleanedUp.getAndSet(true)
    if (!alreadyShutdown) {
      try {
        Runtime.getRuntime.removeShutdownHook(shutDownThread)
      } catch {
        case e: IllegalStateException => // Normal if already shutting down
      }
      collector.shutdown()
    }
  }

  def generateReport(): String = {
    RunningIasTools.logger.debug("Generating report")

    val hbType: Array[HeartbeatProducerType] = HeartbeatProducerType.values()
    val types = (for tp <- hbType yield tp).sorted
    val ret = new mutable.StringBuilder()

    types.foreach(tp => {
      val hbsOfType = collector.getHbsOfType(tp)
      ret.append(s"$tp: ")
      hbsOfType.foreach(hb => {
        val parts: Array[String] = hb.hb.id.split(":")
        ret.append(parts(0))
        if (verboseOn) ret.append(s"@${hb.hb.hostName}:${hb.status}")
        ret.append(' ')
      })
      ret.append('\n')
    })
    ret.toString()
  }

  def process(): Unit = {
    RunningIasTools.logger.debug(s"Collecting HBs for $collectingTime msecs...")
    collector.collectHbsFor(Duration.ofMillis(collectingTime))
    RunningIasTools.logger.debug(s"Collected ${collector.size} HBs in the past $collectingTime msecs...")
  }
}

/** Companion object */
object RunningIasTools {

  /** Build the usage message */
  val cmdLineSyntax: String = "iasRunningTools [-h|--help] [-b|--brokers] [-t|--timeout-msec] [-x|--logLevel log level]"

  /** Default log level */
  val DEFAULT_LOG_LEVEL = LogLevelDao.WARN

  /** Default timeut 10 secs */
  val DEFAULT_TIMEOUT = 10000L

  /** Default kafka brokers */
  val DEFAULT_BROKERS: String = KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS

  /** Min time interval to collect the HBs */
  val MIN_COLLECTING_TIME = 1000L

  /** The logger */
  val logger: Logger = IASLogger.getLogger(RunningIasTools.getClass)

  /** Command line parameters to pass to the object */
  case class CmdLineParams(
                          brokers: String,
                          timeout: Long,
                          verbose: Boolean,
                          logLevel: LogLevelDao
  )


  /**
   * Parse the command line.
   *
   * If help is requested, prints the message and exits.
   *
   * @param args The params read from the command line
   * @return the parameters to pass to the object
   */
  def parseCommandLine(args: Array[String], options: Options): CmdLineParams = {
    val parser: CommandLineParser = new DefaultParser
    val cmdLineParseAction = Try(parser.parse(options,args))
    if (cmdLineParseAction.isFailure) {
      val e = cmdLineParseAction.asInstanceOf[Failure[Exception]].exception
      println(s"$e\n")
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(-1)
    }

    val cmdLine: CommandLine = cmdLineParseAction.asInstanceOf[Success[CommandLine]].value
    val help: Boolean = cmdLine.hasOption('h')
    val verbose: Boolean = cmdLine.hasOption('v')

    val brokers: String = Option(cmdLine.getOptionValue('b')).getOrElse(DEFAULT_BROKERS)
    val timeout: Long =  Option(cmdLine.getOptionValue('t'))
      .map(java.lang.Long.parseLong)
      .getOrElse(DEFAULT_TIMEOUT)

    val logLevel: LogLevelDao = Option(cmdLine.getOptionValue('x'))
      .map(LogLevelDao.valueOf)
      .getOrElse(DEFAULT_LOG_LEVEL)

    if (help) {
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(0)
    }

    CmdLineParams(brokers, timeout, verbose, logLevel)
  }

  /** Application: run the RunningIasTools */
  def main(args: Array[String]): Unit = {

    // the options to parse in the command line
    val options: Options = new Options
    options.addOption("h", "help",false,"Print help and exit")
    options.addOption("b", "brokers",true,"Comma separated list of kafka brokers (default localhost:9092)")
    options.addOption("t", "timeout-msec",true,s"Time to wait collecting HBs (msecs>${RunningIasTools.MIN_COLLECTING_TIME}, default 10000)")
    options.addOption("v", "verbose", false, "Verbose mode ON (does not affect logging but the output")
    options.addOption("x", "logLevel", true, "Set the log level (TRACE, DEBUG, INFO, WARN, ERROR; default WARN)")

    val params = Try { parseCommandLine(args, options) }
    params match {
      case Success(value) =>
        IASLogger.setRootLogLevel(value.logLevel.toLoggerLogLevel)
        IASLogger.setLogLevel("org.apache.kafka", LogLevelDao.ERROR.toLoggerLogLevel)
        val runner = RunningIasTools(value)
        runner.setup()
        runner.process()
        val out = runner.generateReport()
        runner.shutdown()
        println(out)
      case Failure(exception) =>
        logger.error("Error parsing the command line",exception)
        println()
        new HelpFormatter().printHelp(cmdLineSyntax, options)
    }
  }
}
