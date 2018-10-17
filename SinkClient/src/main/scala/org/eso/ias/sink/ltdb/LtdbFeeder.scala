package org.eso.ias.sink.ltdb

import java.util

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, CommandLineParser, DefaultParser, HelpFormatter, Options}
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbFiles, CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.{IasDao, IasioDao, LogLevelDao}
import org.eso.ias.cdb.rdb.RdbReader
import org.eso.ias.dasu.subscriber.{InputSubscriber, KafkaSubscriber}
import org.eso.ias.heartbeat.HbProducer
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.{IasValueProcessor, ValueListener}
import org.eso.ias.types.{IASValue, Identifier, IdentifierType}

import scala.collection.JavaConverters
import scala.collection.mutable.{Map => MutableMap}
import scala.util.{Failure, Success, Try}

/**
  * The feeder of IASIOs in the LTDB.
  *
  * It applies the on change policy before delegating the [[DatabaseFeeder]] to save
  * the records in the database.
  *
  * @param id The identifier to distinguish between many listeners int he same processor
  *           Mainly used for logging messages
  * @param onChange if true stores IASIos on change otherwise stores all the IASIOs received
  *                 from the BSDB
  * @param feeder the feeder that effectively stores IASValues in the LTDB
  */
class LtdbFeeder(id: String, val onChange: Boolean, val feeder: DatabaseFeeder) extends ValueListener(id) {
  require(Option(id).isDefined && id.nonEmpty, "Invalid empty identifier")
  require(Option(feeder).isDefined,"The dtabase feeder must be defined")

  /**
    * The map to implemente the on change
    *
    * If onChange is requested, each received value is compared against the value in this map
    * and published only if it changed in value, mode or reliability
    */
  val iasiosToStore: MutableMap[String,IASValue[_]] = MutableMap.empty

  /**
    * Initialization
    */
  override protected def init(): Unit = {
    feeder.init()
    LtdbFeeder.logger.debug("LtdbFeeder [{}] initialized",id)
  }

  /**
    * Free all the allocated resources
    */
  override protected def close(): Unit = {
    Try(feeder.close()) match {
      case Failure(exception) =>
        LtdbFeeder.logger.error("Error shutting down the database", exception)
      case Success(value) =>
    }
    LtdbFeeder.logger.debug("LtdbFeeder [{}] closed",id)
  }

  /**
    * Process the IasValues read from the BSDB by delegating to the database feeder.
    *
    * If on change is requested, than it dfiscard all the values that have not changed since
    * the last invocation.
    *
    * @param iasValues the values read from the BSDB
    */
  override protected def process(iasValues: List[IASValue[_]]): Unit = {
    val valuesToSend: List[IASValue[_]] = {
      if (!onChange) iasValues
      else synchronized {
        iasValues.filter(value => {
          val valueInMap = iasiosToStore.get(value.id)
          valueInMap match {
            case None => // The value is not in the map i.e. never arrived before
              iasiosToStore.put(value.id, value)
              true
            case Some(v) =>
              if (v.value != value.value || v.mode != value.mode || v.iasValidity != value.iasValidity) {
                // Value changed
                iasiosToStore.put(value.id, value)
                true
              } else {
                // Did not change: filtered out
                false
              }
          }
        })
      }
    }

    Try(feeder.store(valuesToSend)) match {
      case Success(value) =>
      case Failure(exception) =>
        LtdbFeeder.logger.error("Exception got wile storing record in the database",exception)
    }
  }
}

/** Companion object */
object LtdbFeeder {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(LtdbFeeder.getClass)

  /** The name of the property to set the on-change mode */
  val OnChangePropName: String = "org.eso.ias.ltdb.onchange"

  /** On change is disabled by default */
  val defaultOnChangeMode: Boolean = false

  /** True if on-change is activate; false otherwise */
  lazy val onChangeModeEnable:  Boolean =
    java.lang.Boolean.valueOf(System.getProperty(OnChangePropName,defaultOnChangeMode.toString))

  /** The name of the property to set the TTL */
  val TimeToLeavePropName: String = "org.eso.ias.ltdb.ttl"

  /** The default value of the TTL in hours */
  val TimeToLeaveDefault = 24

  /** The TTL in hours */
  lazy val TimeToLeave: Integer = Integer.valueOf(System.getProperty(TimeToLeavePropName,TimeToLeaveDefault.toString))

  /** Build the usage message */
  val CmdLineSyntax: String = "LtdbFeeder Feeder-ID [-h|--help] [-j|-jcdb JSON-CDB-PATH] [-x|--logLevel log level]"

  /**
    * Parse the command line.
    *
    * If help is requested, prints the message and exits.
    *
    * @param args The params read from the command line
    * @return a tuple with the Id of the feeder, the path of the cdb and the log level dao
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
      println("Error parsing the command line: \n"+e)
      new HelpFormatter().printHelp(CmdLineSyntax, options)
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
          new HelpFormatter().printHelp(CmdLineSyntax, options)
          System.exit(-1)
          None
      }
    }

    val remaingArgs = cmdLine.getArgList

    val feederId = if (remaingArgs.isEmpty) None else Some(remaingArgs.get(0))

    if (!help && feederId.isEmpty) {
      println("Missing feeder ID")
      new HelpFormatter().printHelp(CmdLineSyntax, options)
      System.exit(-1)
    }
    if (help) {
      new HelpFormatter().printHelp(CmdLineSyntax, options)
      System.exit(0)
    }

    val ret = (feederId, jcdb, logLvl)
    LtdbFeeder.logger.info("Params from command line: jcdb={}, logLevel={} supervisor ID={}",
      ret._2.getOrElse("Undefined"),
      ret._3.getOrElse("Undefined"),
      ret._1.getOrElse("Undefined"))
    ret
  }

  /**
    * Application: run the LTDB feeder
    *
    * @param args command line args
    */
  def main(args: Array[String]): Unit = {
    val parsedArgs = parseCommandLine(args)
    require(parsedArgs._1.nonEmpty, "Missing identifier in command line")

    /** The ID of the feeder */
    val feederId: String = parsedArgs._1.get

    /** The path of the JSON CDB if defined */
    val jsonCdbPath: Option[String] = parsedArgs._2

    /** The l;og level from the comand line, if defined */
    val logLevelFromCommandLine: Option[LogLevelDao] =parsedArgs._3

    // The identifier of the feeder
    val identifier: Identifier = new Identifier(feederId, IdentifierType.SINK, None)

    /** The configuration of the IAS and the IASIOs */
    val (iasDao: IasDao, iasioDaos: List[IasioDao]) = {
      val reader: CdbReader = {
        if (jsonCdbPath.isDefined) {
          val cdbPath = jsonCdbPath.get
          logger.info("Using JSON CDB @ {}",cdbPath)
          val cdbFiles: CdbFiles = new CdbJsonFiles(cdbPath)
          new JsonReader(cdbFiles)
        } else {
          logger.info("Using RDB CDB")
          new RdbReader()
        }
      }
      val iasDao: IasDao = {
        val iasFromCdb=reader.getIas
        if (!iasFromCdb.isPresent) {
          logger.error("Error getting IAS configuration from CDB")
          System.exit(-1)
        }
        iasFromCdb.get()
      }

      val iasioDaos: List[IasioDao] = {
        val temp: util.Set[IasioDao] = reader.getIasios.orElseThrow(() => new IllegalArgumentException("IasDaos not found in CDB"))
        JavaConverters.asScalaSet(temp).toList
      }

      Try(reader.shutdown()) match {
        case Success(value) =>
        case Failure(exception) =>
          logger.warn("Error closing the CDB",exception)
      }
      (iasDao, iasioDaos)
    }

    // Set the log level
    val actualLogLevel = IASLogger.setLogLevel(
      logLevelFromCommandLine.map(_.toLoggerLogLevel),
      Option(iasDao.getLogLevel).map(_.toLoggerLogLevel),
      None)
    logger.info("Log level set to {}",actualLogLevel.getOrElse("default from logback configuration").toString)

    /**
      * Kafka brokers taken from the java property or, if not present, from the CDB.
      * If none of them is present, uses the default in [[KafkaHelper]]
      */
    val kafkaBrokers = {
      val brokersFromProps = Option(System.getProperty(KafkaHelper.BROKERS_PROPNAME))
      if (brokersFromProps.isEmpty) {
        logger.debug("Getting BSDB URL from the IAS in the CDB")
        iasDao.getBsdbUrl
      } else {
        logger.debug("Using kafka broker URL from java properties")
        brokersFromProps.get
      }
    }
    logger.info("Kafka brokers: {}",kafkaBrokers)

    val hbProducer: HbProducer = {
      val kafkaServers = System.getProperties.getProperty(KafkaHelper.BROKERS_PROPNAME,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
      new HbKafkaProducer(feederId + "HBSender",kafkaServers,new HbJsonSerializer())
    }
    logger.debug("HB producer instantiated")

    val inputsProvider: InputSubscriber = KafkaSubscriber(feederId,None,Option(kafkaBrokers),System.getProperties)
    logger.debug("IAS values consumer instantiated")

    // Store records in Cassandra
    val databaseFeeder = new CassandraFeeder(TimeToLeave)

    /** The feeder of data to Cassandra */
    val ltdbFeeder = new LtdbFeeder(feederId,onChangeModeEnable, databaseFeeder)

    val valuesProcessor: IasValueProcessor = new IasValueProcessor(
      identifier,
      List(ltdbFeeder),
      hbProducer,
      inputsProvider,
      iasDao,
      iasioDaos)
    logger.debug("IAS values processor instantiated")

    // Start
    logger.info("Starting the loop...")
    valuesProcessor.init()
  }
}
