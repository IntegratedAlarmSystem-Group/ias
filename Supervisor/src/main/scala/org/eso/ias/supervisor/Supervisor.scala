package org.eso.ias.supervisor

import ch.qos.logback.classic.Level
import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, CommandLineParser, DefaultParser, HelpFormatter, Options}
import org.eso.ias.cdb.pojos.*
import org.eso.ias.cdb.topology.TemplateHelper
import org.eso.ias.cdb.{CdbReader, CdbReaderFactory}
import org.eso.ias.command.CommandManager
import org.eso.ias.command.kafka.CommandManagerKafkaImpl
import org.eso.ias.dasu.publisher.{KafkaPublisher, OutputPublisher}
import org.eso.ias.dasu.subscriber.{InputSubscriber, InputsListener, KafkaSubscriber}
import org.eso.ias.dasu.{Dasu, DasuImpl}
import org.eso.ias.heartbeat.publisher.HbKafkaProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{HbEngine, HbProducer, HeartbeatProducerType, HeartbeatStatus}
import org.eso.ias.kafkautils.{KafkaHelper, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASValue, Identifier, IdentifierType}
import org.eso.ias.utils.ISO8601Helper

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.jdk.javaapi.CollectionConverters
import scala.util.{Failure, Success, Try}

/**
  * A Supervisor is the container to run several DASUs into the same JVM.
  *
  * The Supervisor blindly forward inputs to each DASU and sends the outpts to the BSDB
  * without adding any other heuristic: things like updating validities when an input
  * is not refreshed are not part of the Supervisor.
  *
  * The Supervisor gets IASIOs from a InputSubscriber and publishes
  * IASValues to the BSDB by means of a OutputPublisher.
  * The Supervisor itself is the publisher and subscriber for the DASUs i.e.
  * the Supervisor acts as a bridge:
  *  * IASIOs read from the BSDB are forwarded to the DASUs that need them as input:
  *    the Supervisor has its own subscriber to receive values from the BSDB that
  *    are then forwarded to each DASU for processing
  *  * values produced by the DASUs are forwarded to the BSDB: the DASUs publishes the output
  *    they produce to the supervisor that, in turn, forward each of them to its own publisher.
  *
  * The same interfaces, InputSubscriber and OutputPublisher,
  * are used by DASUs and Supervisors in this way a DASU can be easily tested
  * directly connected to Kafka (for example) without the need to have
  * it running into a Supervisor.
  *
  * DASUs are built by invoking the dasufactory passed in the constructor:
  * test can let the Supervisor run with their mockup implementation of a DASU.
  *
  * If not defined, the constructor builds the HB producer, the output publisher and the command manager
  * using the passed SimpleStringProducer. All those producers are those used in operatons i.e. sending data
  * to the kafka servers.
  * The constructor allows to override this implementation passing special producers, a feature useful for testing
  *
  * @param supervisorIdentifier the identifier of the Supervisor
  * @param kafkaBrokers the string with kafka brokers
  * @param stringProducerOpt the string producer to push in kafka
  * @param outputPublisherOpt the publisher to send the output
  * @param inputSubscriber the subscriber to get events to be processed
  * @param hbProducerOpt the subscriber to send heartbeats
  * @param commandManagerOpt the command manager
  * @param cdbReader the CDB reader to get the configuration of the DASU from the CDB
  * @param dasuFactory: factory to build DASU
  * @param logLevelFromCommandLine The log level from the command line;
  *                                None if the parameter was not set in the command line
  * @author acaproni
  */

class Supervisor private (
                  supervisorIdentifier: Identifier,
                  kafkaBrokers: String,
                  private val stringProducerOpt: Option[SimpleStringProducer],
                  outputPublisherOpt: Option[OutputPublisher],
                  inputSubscriber: InputSubscriber,
                  hbProducerOpt: Option[HbProducer],
                  commandManagerOpt: Option[CommandManager],
                  cdbReader: CdbReader,
                  dasuFactory: (DasuDao, Identifier, OutputPublisher, InputSubscriber) => Dasu,
                  logLevelFromCommandLine: Option[LogLevelDao])
  extends InputsListener with InputSubscriber with  OutputPublisher with AutoCloseable {

  require(Option(supervisorIdentifier).isDefined,"Invalid Supervisor identifier")
  require(Option(kafkaBrokers).isDefined && !kafkaBrokers.isEmpty, "Invalid null/epty string of kafka brokers")
  require(Option(stringProducerOpt).isDefined,"Invalid null string producer")
  require(Option(outputPublisherOpt).isDefined,"Invalid null output publisher")
  require(Option(inputSubscriber).isDefined,"Invalid input subscriber")
  require(Option(hbProducerOpt).isDefined,"Invalid null HB producer")
  require(Option(commandManagerOpt).isDefined,"Invalid null command manager")
  require(Option(cdbReader).isDefined,"Invalid CDB reader")
  require(Option(logLevelFromCommandLine).isDefined,"Invalid log level")

  if (stringProducerOpt.isEmpty &&
    (outputPublisherOpt.isEmpty || hbProducerOpt.isEmpty || commandManagerOpt.isEmpty)) {
    throw new IllegalArgumentException("Cannot build hb producer, command manager and IASIOs producer if the simple stirng is empty")
  }
  
  /** The ID of the Supervisor */
  val id: String = supervisorIdentifier.id

  Supervisor.logger.info("Building Supervisor [{}] with fullRunningId [{}]",id,supervisorIdentifier.fullRunningID)

  val iasDao: IasDao =
    Try (cdbReader.getIas ) match {
      case Success(value) => value.orElseThrow(() => new Exception("IasDao not found"))
      case Failure(exception) => throw new Exception("Failure reading IAS from CDB",exception)
    }
  
  /** The heartbeat Engine */
  val hbProducer: HbProducer =
    hbProducerOpt.getOrElse(new HbKafkaProducer(stringProducerOpt.get,supervisorIdentifier.id,new HbJsonSerializer()))
  val hbEngine: HbEngine = HbEngine(supervisorIdentifier.id,HeartbeatProducerType.SUPERVISOR,iasDao.getHbFrequency,hbProducer)

  /**
    * The refresh rate in mseconds
    *
    * The refresh rate is used only to detetct if the Supervisor is too slow
    * processing values.
    * Auto refresh is, in fact, implemented by DASUs
    */
  val refreshRate: Long = TimeUnit.MILLISECONDS.convert(iasDao.getRefreshRate, TimeUnit.SECONDS)

  
  // Get the configuration of the supervisor from the CDB
  val supervDao : SupervisorDao = {
    val supervDaoOpt = cdbReader.getSupervisor(id)
    require(supervDaoOpt.isPresent,"Supervisor ["+id+"] configuration not found on cdb")
    supervDaoOpt.get
  }
  Supervisor.logger.info("Supervisor [{}] configuration retrieved from CDB",id)

  // Set the log level
  {
    val iasLogLevel: Option[Level] = Option(iasDao.getLogLevel).map(_.toLoggerLogLevel)
    val supervLogLevel: Option[Level] =  Option(supervDao.getLogLevel).map(_.toLoggerLogLevel)
    val cmdLogLevel: Option[Level] = logLevelFromCommandLine.map(_.toLoggerLogLevel)
    val level: Option[Level] = IASLogger.setLogLevel(cmdLogLevel,iasLogLevel,supervLogLevel)
    // Log a message that, depending on the log level can be discarded
    level.foreach(l => Supervisor.logger.info("Log level set to {}",l.toString))
  }
  
  /**
   * Gets the definitions of the DASUs to run in the Supervisor from the CDB
   */
  val dasusToDeploy: Set[DasuToDeployDao] = CollectionConverters.asScala(cdbReader.getDasusToDeployInSupervisor(id)).toSet
  require(dasusToDeploy.nonEmpty,"No DASUs to run in Supervisor "+id)
  Supervisor.logger.info("Supervisor [{}], {} DASUs to run: {}",
      id,
    dasusToDeploy.size.toString,
    dasusToDeploy.map(d => d.getDasu.getId).mkString(", "))
  
  // Build the kafka producer of IASIOs
  val outputPublisher: OutputPublisher =
    outputPublisherOpt.getOrElse(KafkaPublisher(supervisorIdentifier.id,None,stringProducerOpt.get,None))

  // Get the DasuDaos from the set of DASUs to deploy:
  // the helper transform the templated DASUS into normal ones
  val dasuDaos: Set[DasuDao] = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
  assert(dasuDaos.size==dasusToDeploy.size)

  // Get the IDs of the TFs running in the Supervisor: upon receiving a TF_CHANGED command and if such TF is used
  // by at least one ASCE, the Supervisor needs to restart
  lazy val tfIDs: List[String] =
    dasuDaos.foldLeft(Set.empty[String]) ( (asces, dasu) =>
      asces ++ CollectionConverters.asScala(dasu.getAsces).map(_.getTransferFunction.getClassName)
    ).toList

  dasuDaos.foreach(d => Supervisor.logger.info("Supervisor [{}]: building DASU from DasuDao {}",id,d.toString))
  
  // Build all the DASUs
  val dasus: Map[String, Dasu] = dasuDaos.foldLeft(Map.empty[String,Dasu])((m, dasuDao) => 
    m + (dasuDao.getId -> dasuFactory(dasuDao,supervisorIdentifier,this,this)))
  
  /**
   * The IDs of the DASUs instantiated in the Supervisor
   */
  val dasuIds: Set[String] = dasuDaos.map(_.getId)
  Supervisor.logger.info("Supervisor [{}] built {} DASUs: {}",id, dasus.size.toString,dasuIds.mkString(", "))
  
  /**
   * Associate each DASU with the Set of inputs it needs.
   * 
   * the key is the ID of the DASU, the value is 
   * the set of inputs to send to the DASU
   */
  val iasiosToDasusMap: Map[String, Set[String]] = startDasus()
  Supervisor.logger.info("Supervisor [{}] associated IASIOs IDs to DASUs", id)
  
  val cleanedUp: AtomicBoolean = new AtomicBoolean(false) // Avoid cleaning up twice
  val shutDownThread: Thread =addsShutDownHook()
  
  /** Flag to know if the Supervisor has been started */
  val started = new AtomicBoolean(false)

  val statsLogger: SupervisorStatistics = new SupervisorStatistics(id,dasuIds)

  /** The command manager to get and execute commands */
  val commandManager: CommandManager =
    commandManagerOpt.getOrElse(new CommandManagerKafkaImpl(supervisorIdentifier.id,kafkaBrokers,stringProducerOpt.get))

  /** The command executor that executes the commands received from the cmd topic */
  val cmdExecutor: SupervisorCmdExecutor = new SupervisorCmdExecutor(tfIDs, dasus)

  Supervisor.logger.info("Supervisor [{}] built",id)

  /**
   * Constructor that allows to override standard kafka producer with the
   * passed in the parameters.
   *
   * This constructor is intended for testing purposes.
   *
   * @param supervisorIdentifier the identifier of the Supervisor
   * @param outputPublisher the publisher to send the output
   * @param inputSubscriber the subscriber getting events to be processed
   * @param hbProducer the subscriber to send heartbeats
   * @param cdbReader the CDB reader to get the configuration of the DASU from the CDB
   * @param dasuFactory: factory to build DASU
   * @param logLevelFromCommandLine The log level from the command line;
   *                                None if the parameter was not set in the command line
   * @return  a new Supervisor
   */
  def this(
             supervisorIdentifier: Identifier,
             outputPublisher: OutputPublisher,
             inputSubscriber: InputSubscriber,
             hbProducer: HbProducer,
             commandManager: CommandManager,
             cdbReader: CdbReader,
             dasuFactory: (DasuDao, Identifier, OutputPublisher, InputSubscriber) => Dasu,
             logLevelFromCommandLine: Option[LogLevelDao]) = {
    this(
      supervisorIdentifier,
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      None,
      Option(outputPublisher),
      inputSubscriber,
      Option(hbProducer),
      Option(commandManager),
      cdbReader,
      dasuFactory,
      logLevelFromCommandLine
    )
  }

  /**
   * Factory method to build a Supervisor with kafka producers and consumers
   *
   * This methods builds the Supervisor used in operation and connected with Kafka server.
   *
   * @param supervisorIdentifier the identifier of the Supervisor
   * @param kafkaBrokers the string with kafka brokers
   * @param inputSubscriber the subscriber to get events to be processed
   * @param cdbReader the CDB reader to get the configuration of the DASU from the CDB
   * @param dasuFactory: factory to build DASU
   * @param logLevelFromCommandLine The log level from the command line;
   *                                None if the parameter was not set in the command line
   * @return a new Supervisor
   */
  def this(
             supervisorIdentifier: Identifier,
             kafkaBrokers: String,
             inputSubscriber: InputSubscriber,
             cdbReader: CdbReader,
             dasuFactory: (DasuDao, Identifier, OutputPublisher, InputSubscriber) => Dasu,
             logLevelFromCommandLine: Option[LogLevelDao]) = {
    this(
      supervisorIdentifier,
      kafkaBrokers,
      Option(new SimpleStringProducer(kafkaBrokers,supervisorIdentifier.id)),
      None,
      inputSubscriber,
      None,
      None,
      cdbReader,
      dasuFactory,
      logLevelFromCommandLine
    )
  }


  /**
   * Start each DASU and gets the list of inputs it needs to forward to the ASCEs
   * 
   * Invoking start to a DASU triggers the initialization of its input subscriber
   * that it is implemented by this Supervisor so, ultimately, 
   * each DASU calls Supervisor#startSubscriber.
   * This method, calls Dasu#start() just to be and independent of the
   * implementation of Dasu#start() itself.
   */
  private def startDasus(): Map[String, Set[String]] = {
    dasus.values.foreach(_.start())
    
    val fun = (m: Map[String, Set[String]], d: Dasu) => m + (d.id -> d.getInputIds())
    dasus.values.foldLeft(Map.empty[String, Set[String]])(fun)
  }
  
  /**
   * Enable or diable the auto-refresh of the outputs in the DASUs
   * 
   * @param enable if true enable the autorefresh, otherwise disable the autorefresh
   */
  def enableAutoRefreshOfOutput(enable: Boolean): Unit = {
      dasus.values.foreach(dasu => dasu.enableAutoRefreshOfOutput(enable))
  }
  
  /**
   * Start the loop:
   * - get events from the BSDB
   * - forward events to the DASUs
   * 
   * @return Success if the there were no errors starting the supervisor, 
   *         Failure otherwise 
   */
  def start(): Try[Unit] = {
    val alreadyStarted = started.getAndSet(true) 
    if (!alreadyStarted) {
      Supervisor.logger.debug("Starting Supervisor [{}]",id)
      statsLogger.start()
      stringProducerOpt.foreach(_.setUp())
      // Initialize the consumer of IASIOs
      inputSubscriber.initializeSubscriber()
      // Initialize the output producer
      outputPublisher.initializePublisher()

      hbEngine.start()
      dasus.values.foreach(dasu => dasu.enableAutoRefreshOfOutput(true))
      val inputsOfSupervisor = dasus.values.foldLeft(Set.empty[String])( (s, dasu) => s ++ dasu.getInputIds())
      commandManager.start(cmdExecutor,this)
      inputSubscriber.startSubscriber(this, inputsOfSupervisor).flatMap(s => {
        Try{
          Supervisor.logger.debug("Supervisor [{}] started",id)
          hbEngine.updateHbState(HeartbeatStatus.RUNNING)}})
    } else {
      Supervisor.logger.warn("Supervisor [{}] already started",id)
      Failure(new Exception("Supervisor already started"))
    }
  }

  /**
   * Release all the resources
   */
  def close(): Unit = synchronized {

    val alreadyCleaned = cleanedUp.getAndSet(true)
    if (!alreadyCleaned) {
      Supervisor.logger.debug("Cleaning up supervisor [{}]", id)
      statsLogger.cleanUp()
      hbEngine.updateHbState(HeartbeatStatus.EXITING)
      Supervisor.logger.debug("Releasing DASUs running in the supervisor [{}]", id)
      dasus.values.foreach(_.cleanUp())

      Supervisor.logger.debug("Supervisor [{}]: releasing the subscriber", id)
      Try(inputSubscriber.cleanUpSubscriber())
      Supervisor.logger.debug("Supervisor [{}]: releasing the publisher", id)
      Try(outputPublisher.cleanUpPublisher())
      hbEngine.shutdown()
      Supervisor.logger.debug("Supervisor [{}]: closing the kafka string producer", id)
      stringProducerOpt.foreach(_.tearDown())
      Supervisor.logger.info("Supervisor [{}]: cleaned up", id)
    }
  }

    /** Adds a shutdown hook to cleanup resources before exiting */
  private def addsShutDownHook(): Thread = {
    val t = new Thread() {
        override def run(): Unit = {
          Supervisor.logger.info("Shutdown hook is closing the Supervisor {}",supervisorIdentifier.id)
          close()
        }
    }
    Runtime.getRuntime.addShutdownHook(t)
    t
  }
  
  /** 
   *  Notify the DASUs of new inputs received from the consumer
   *  
   *  @param iasios the inputs received
   */
  override def inputsReceived(iasios: Iterable[IASValue[_]]): Unit = {
    
    val receivedIds = iasios.map(i => i.id)
    statsLogger.numberOfInputsReceived(receivedIds.size)

    Supervisor.logger.debug("New inputs to send to DASUs: {}", receivedIds.mkString(","))

    // Check if the Supervisor is too slow to cope with the flow of values published in the BSDB.
    //
    // The check is done by comparing the current time with the moment the value has been
    // pushed in the BSDB.
    // Normally a new value arrives after the refresh time: the check assumes that
    // there is a problem if the point in time when an input has been published in the kafka
    // topic is greater than 2 times the refresh rate
    //
    // The Supervisor does not start any action if a delay is detected: it only emits a waring.
    val now = System.currentTimeMillis()
    val oldIasValue = iasios.find(iasValue => now-iasValue.sentToBsdbTStamp.get()>2*refreshRate)
    if (oldIasValue.isDefined) {
      Supervisor.logger.warn("Supervisor too slow: input [{}] sent to BSDB at {} but scheduled for processing only now!",
        oldIasValue.get.id,ISO8601Helper.getTimestamp(oldIasValue.get.sentToBsdbTStamp.get()))
    }

    
    dasus.values.foreach(dasu => {
      val iasiosToSend = iasios.filter(iasio => iasiosToDasusMap(dasu.id).contains(iasio.id))

      statsLogger.numOfInputsOfDasu(dasu.id,iasiosToSend.size)
      if (iasiosToSend.nonEmpty) {
        dasu.inputsReceived(iasiosToSend)

        Supervisor.logger.debug("Inputs sent to DASU [{}] for processing: {}",
          dasu.id,
          iasiosToSend.map(_.id).mkString(","))
      } else {
        Supervisor.logger.debug("No inputs for DASU [{}]",dasu.id)
      }
    })
    statsLogger.supervisorPropagationTime(System.currentTimeMillis()-now)

  }
  
  /** 
   *  The Supervisor acts as publisher for the DASU
   *  by forwarding IASIOs to its own publisher.
   *  The initialization has already been made by the supervisor 
   *  so this method, invoke by each DASU,
   *  does nothing and always return success. 
   *  
   *  @return Success or Failure if the initialization went well 
   *          or encountered a problem  
   */
  def initializePublisher(): Try[Unit] = Success(())
  
  /**
   * The Supervisor acts as publisher for the DASU
   * by forwarding IASIOs to its own publisher.
   * The clean up will be done by by the supervisor on its own publisher 
   * so this method, invoked by each DASU, 
   * does nothing and always return success. 
   *  
   *  @return Success or Failure if the clean up went well 
   *          or encountered a problem  
   */
  def cleanUpPublisher(): Try[Unit] = Success(())
  
  /**
   * The Supervisor acts as publisher for the DASU
   * by forwarding IASIOs to its own publisher.
   * 
   * @param iasio the not IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  def publish(iasio: IASValue[_]): Try[Unit] = outputPublisher.publish(iasio)
  
  /** 
   *  The Supervisor has its own subscriber so this initialization,
   *  invoked by each DASU, does nothing but returning Success.
   */
  def initializeSubscriber(): Try[Unit] = Success(())
  
  /** 
   *  The Supervisor has its own subscriber so this  clean up 
   *  invoked by each DASU, does nothing but returning Success. 
   */
  def cleanUpSubscriber(): Try[Unit] = Success(())
  
  /**
   * The Supervisor has its own subscriber to get events from: the list of
   * IDs to be accepted is composed of the IDs accepted by each DASUs.
   * 
   * Each DASU calls this method when ready to accept IASIOs; the Supervisor
   * - uses the passedInputs to tune its list of accepted IDs.
   * - uses the passed listener to forward to each DAUS the IASIOs it receives  
   * 
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   */
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
    Success(())
  }
}

object Supervisor {
  
  /** The logger */
  val logger: Logger = IASLogger.getLogger(Supervisor.getClass)

  /** Build the usage message */
  val cmdLineSyntax: String = "Supervisor Supervisor-ID [-h|--help] [-j|-jcdb JSON-CDB-PATH] [-x|--logLevel log level]"

  /**
    * Parse the command line.
    *
    * If help is requested, prints the message and exits.
    *
    * @param args The params read from the command line
    * @return a tuple with the Id of the supervisor, the path of the cdb and the log level dao
    */
  def parseCommandLine(args: Array[String]): (Option[String],  Option[String], Option[LogLevelDao]) = {
    val options: Options = new Options
    options.addOption("h", "help",false,"Print help and exit")
    options.addOption("j", "jCdb", true, "Use the JSON Cdb at the passed path")
    options.addOption("c", "cdbClass", true, "Use an external CDB reader with the passed class")
    options.addOption("x", "logLevel", true, "Set the log level (TRACE, DEBUG, INFO, WARN, ERROR)")

    val parser: CommandLineParser = new DefaultParser
    val cmdLineParseAction = Try(parser.parse(options,args))
    if (cmdLineParseAction.isFailure) {
      val e = cmdLineParseAction.asInstanceOf[Failure[Exception]].exception
      println(s"$e\n")
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

    val supervId = if (remaingArgs.isEmpty) None else Some(remaingArgs.get(0))

    if (!help && supervId.isEmpty) {
      println("Missing Supervisor ID")
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(-1)
    }
    if (help) {
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(0)
    }

    val ret = (supervId, jcdb, logLvl)
    Supervisor.logger.info("Params from command line: jCdb={}, logLevel={} supervisor ID={}",
      ret._2.getOrElse("Undefined"),
      ret._3.getOrElse("Undefined"),
      ret._1.getOrElse("Undefined"))
    ret
  }

  /**
   *  Application: run a Supervisor with the passed ID and
   *  kafka producer and consumer.
   *
   *  Kill to terminate.
   */
  def main(args: Array[String]): Unit = {
    val parsedArgs = parseCommandLine(args)
    require(parsedArgs._1.nonEmpty, "Missing identifier in command line")

    val supervisorId = parsedArgs._1.get

    val reader: CdbReader = CdbReaderFactory.getCdbReader(args)
    reader.init()

    /** 
     *  Refresh rate and validityThreshold: it uses the first defined ones:
     *  1 java properties,
     *  2 CDB
     *  3 default
     */
    val (refreshRate, validityThreshold,kafkaBrokers) = {

      val iasDaoOpt = reader.getIas

      val fromCdb = if (iasDaoOpt.isPresent) {
        logger.debug("IAS configuration read from CDB")
        (iasDaoOpt.get.getRefreshRate,iasDaoOpt.get.getValidityThreshold,Option(iasDaoOpt.get.getBsdbUrl))
      } else {
        logger.warn("IAS not found in CDB: using default values for auto send time interval ({}) and validity threshold ({})",
          AutoSendTimeIntervalDefault,ValidityThresholdDefault)
        (AutoSendTimeIntervalDefault,ValidityThresholdDefault,None)
      }
      logger.debug("Using autosend time={}, HB frequency={}, Kafka brokers={}",fromCdb._1,fromCdb._2,fromCdb._3)

      (Integer.getInteger(AutoSendPropName,fromCdb._1),
      Integer.getInteger(ValidityThresholdPropName,fromCdb._2),
      fromCdb._3)
    }
    require(kafkaBrokers.isDefined, "BSDB URL missing in IAS configuration")

    val inputsProvider: InputSubscriber = KafkaSubscriber(supervisorId,None,kafkaBrokers,System.getProperties)

    // The identifier of the supervisor
    val identifier = new Identifier(supervisorId, IdentifierType.SUPERVISOR, None)

    val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) =>
      DasuImpl(dd,i,op,id,refreshRate,validityThreshold)

    // Build the supervisor
    val supervisor = new Supervisor(
      identifier,
      kafkaBrokers.get,
      inputsProvider,
      reader,
      factory,
      parsedArgs._3)

    val started = supervisor.start()

    // Release CDB resources
    reader.shutdown()

    started match {
      case Success(_) => val latch = new CountDownLatch(1); latch.await();
      case Failure(ex) => System.err.println("Error starting the supervisor: "+ex.getMessage)
    }
  }

  /**
   * The name of the property to override the auto send time interval
   * read from the CDB
   */
  val AutoSendPropName = "ias.supervisor.autosend.time"

  /**
   * The default time interval to automatically send the last calculated output
   * in seconds
   */
  val AutoSendTimeIntervalDefault = 5

  /**
   * The name of the property to override the validity threshold
   * read from the CDB
   */
  val ValidityThresholdPropName = "ias.supervisor.autosend.validity.threshold"

  /**
   * The default tolarance in seconds: it is the time added to the auto-refresh before
   * invalidate an input
   */
  val ValidityThresholdDefault = 15
  

}
