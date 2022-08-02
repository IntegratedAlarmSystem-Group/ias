package org.eso.ias.sink.email

import java.time._
import java.util
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, CommandLineParser, DefaultParser, HelpFormatter, Options}
import org.eso.ias.cdb.{CdbReader, CdbReaderFactory}
import org.eso.ias.cdb.pojos._
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.{IasValueProcessor, ValueListener}
import org.eso.ias.supervisor.Supervisor._
import org.eso.ias.types._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.jdk.javaapi.CollectionConverters
import scala.util.{Failure, Success, Try}

/**
  * The NotificationsSender processes the alarms published in the BSDB and sends mails to registered recipients.
  * One email is sent whenever an alarm becomes SET or CLEAR.
  *
  * In addition another email is sent at regular time intervals to notify about the history of the
  * alarm.
  *
  * The sending of notification is delegated to the sender that allows to send notifications by different means
  * (email logs, GSM,...) providing the proper implementations of the Sender trait.
  *
  * @param id identifier of the mail
  * @param sender The sender of the notification
  */
class NotificationsSender(id: String, val sender: Sender) extends ValueListener(id) {
  require(Option(sender).isDefined)

  /**
    * The alarms that have a registered recipients to send notification.
    *
    * Templated alarms are added as they arrive so this map contains an entry
    * for the baseId of the identifier and one entry for each templated alarm
    */
  val alarmsToTrack:  mutable.Map[String, AlarmStateTracker] = mutable.Map.empty

  /** Maps each recipient with the alarms to notify:
    * one user can be registered to be notified of several alarms
    * The key is the address to send notifications to, the value is the list of alarms to notify to the recipient
    */
  val alarmsForUser: mutable.Map[String, List[String]] = mutable.Map.empty

  /**The executor to send notification: all the instances run in a single thread */
  val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor( (runnable: Runnable) =>  {
      require(Option(runnable).isDefined)
      val threadId = "NotificationSenderThread"
      val ret = new Thread(runnable,threadId)
      ret.setDaemon(true)
      ret
  })

  /** The time interval (mins) to send notifications */
  val timeIntervalToSendEmails: Int = Integer.getInteger(
    NotificationsSender.SendEmailsTimeIntervalPropName,
    NotificationsSender.SendEmailsTimeIntervalDefault)
  NotificationsSender.msLogger.info("Will send digest emails every {} minutes",timeIntervalToSendEmails)

  val timeTostart: (Int, Int) = {
    val prop = System.getProperty(
      NotificationsSender.StartTimeOfPeriodicNotificationsPropName,
      NotificationsSender.StartTimeOfPeriodicNotificationsDefault).split(":")
    (Integer.parseInt(prop(0)),Integer.parseInt(prop(1)))
  }
  NotificationsSender.msLogger.info("First digest at {}:{} UTC",timeTostart._1.toString,timeTostart._2.toString)

  /** Start the periodic tasl to send the digests */
  private def startTimer(): Unit = {
    val localNow: ZonedDateTime  = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)

    val nextZone: ZonedDateTime = {
      val temp=localNow.withHour(timeTostart._1).withMinute(timeTostart._2).withSecond(0)
      if(localNow.compareTo(temp) > 0)    temp.plusDays(1)
      else temp
    }

    val duration: Duration = Duration.between(localNow, nextZone)
    val initalDelay = duration.getSeconds
    val timeIntervalSecs = TimeUnit.SECONDS.convert(timeIntervalToSendEmails,TimeUnit.MINUTES)

    executor.scheduleAtFixedRate( () => {
        NotificationsSender.msLogger.debug("TASK")
        sendDigests() },initalDelay,timeIntervalSecs, TimeUnit.SECONDS)
    NotificationsSender.msLogger.debug("Periodic thread scheduled to run in {} secs every {} secs",initalDelay.toString,timeIntervalSecs)
    NotificationsSender.msLogger.info("Periodic send of digest scheduled every {} minutes",timeIntervalToSendEmails.toString)
  }

  /**
    * Initialization: scans the IasioDaos and prepare data structures for tracking state changes and
    * sending notifications
    */
  override protected def init(): Unit = {
    // Scans the IasioDaos to record the alarms for which must send notifications
    val alarmsWithRecipients=iasValuesDaos.values.filter(iasioDao =>
      iasioDao.getIasType==IasTypeDao.ALARM &&
      Option(iasioDao.getEmails).isDefined &&
      !iasioDao.getEmails.trim.isEmpty)
    NotificationsSender.msLogger.info("Tracks and send email notifications for {} alarms",alarmsWithRecipients.size)

    // Saves the base ID of the alarms to send notifications in the map to track the changes of states
    alarmsWithRecipients.foreach({
      iasioD => alarmsToTrack(iasioD.getId)=AlarmStateTracker(iasioD.getId)
    })

    val tempMapOfAlarmsForUsers: mutable.Map[String, ListBuffer[String]] = mutable.Map.empty
    alarmsWithRecipients.foreach(iasioValue => {
      val users = iasioValue.getEmails.split(",").map(_.trim)
      users.foreach(user => {
        tempMapOfAlarmsForUsers.getOrElseUpdate(user,ListBuffer()).append(iasioValue.getId)
      })
    })
    tempMapOfAlarmsForUsers.keys.foreach(user => alarmsForUser(user)=tempMapOfAlarmsForUsers(user).toList)

    startTimer()
  }

  /**
    * Free all the allocated resources
    */
  override protected def close(): Unit = {
    executor.shutdownNow()
    alarmsForUser.clear()
    alarmsToTrack.clear()
  }

  /**
    * Periodically sends the notifications summary of the changes of the statss
    * of all the monitored alarms.
    */
  def sendDigests(): Unit = synchronized {
    // Send one email to each user
    //
    // The email contains the summary of all the alarsm about which the user wants to get notifications
    NotificationsSender.msLogger.debug("Sending digests")
    alarmsForUser.keys.foreach(user => {
      NotificationsSender.msLogger.debug("Sending digest of {} alarms to {}",alarmsForUser(user).mkString(","),user)

      val alarmStates: List[AlarmStateTracker] = alarmsForUser(user).map(alarmId => alarmsToTrack(alarmId))
      NotificationsSender.msLogger.debug("Digests will report the state of {} alarm states for user {}",alarmStates.length,user)
      val sendOp = Try(sender.digestNotify(user,alarmStates))
      if (sendOp.isFailure) {
        NotificationsSender.msLogger.error("Error sending periodic notification to {}",user, sendOp.asInstanceOf[Failure[_]].exception)
      } else {
        NotificationsSender.msLogger.debug("Digest sent to {}",user)
      }
    })
    alarmsToTrack.keys.foreach(id => alarmsToTrack(id)=alarmsToTrack(id).reset())
    NotificationsSender.msLogger.debug("Digests sent to the sender")
  }

  /**
    * Send the notification of the last alarm activation/deactivation
    *
    * @param alarmId The ID of the alarm
    * @param state the state of the alarm
    */
  def notifyAlarm(alarmId: String, state: AlarmState): Unit = {
    require(Option(alarmId).isDefined && !alarmId.isEmpty)
    require(Option(state).isDefined)
    val recipients = iasValuesDaos(Identifier.getBaseId(alarmId)).getEmails.split(",")
    NotificationsSender.msLogger.debug("Sending notifcation of alarm {} status change to {}", alarmId, recipients.mkString(","))
    val sendOp = Try(sender.notify(recipients.map(_.trim).toList, alarmId, state))
    if (sendOp.isFailure) NotificationsSender.msLogger.error("Error sending alarm state notification notification to {}", recipients.mkString(","), sendOp.asInstanceOf[Failure[_]].exception)
  }


  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  override protected def process(iasValues: List[IASValue[_]]): Unit = synchronized {
    NotificationsSender.msLogger.debug("Processing {} values read from BSDB",iasValues.length)

    // The criteria to filter out the IAS values that are not interesting for the notifications
    def accept(iasValue: IASValue[_]): Boolean = {
      iasValue.valueType==IASTypes.ALARM &&
        iasValue.mode!=OperationalMode.MAINTENANCE &&
        alarmsToTrack.contains(Identifier.getBaseId(iasValue.id))
    }

    // Iterates over accepted alarms
    iasValues.filter(accept).foreach(value => {

        val id = value.id

        // Ensure that templated alarms are in the map
        if (Identifier.isTemplatedIdentifier(id) && !alarmsToTrack.keySet.contains(id)) {
          alarmsToTrack(id)=AlarmStateTracker(id)
          print ("Added alarm with ID "+id)
        }

        val alarm = value.asInstanceOf[IASValue[Alarm]].value
        val tStamp: Long = value.productionTStamp.get()
        val validity = value.iasValidity

        // The last state in the current time interval
        val oldState: Option[AlarmState] = alarmsToTrack(id).getActualAlarmState()
        // The state of the last round interval
        val lastRoundState: Option[AlarmState] = alarmsToTrack(id).stateOfLastRound

        // Update the state of the alarm state tracker
        alarmsToTrack(id) = alarmsToTrack(id).stateUpdate(alarm, validity, tStamp)

        (oldState, lastRoundState) match {
          case (None, None) => if (alarm.isSet) notifyAlarm(id,alarmsToTrack(id).getActualAlarmState().get)
          case (Some(x), _) => if (x.alarm != alarm) notifyAlarm(id,alarmsToTrack(id).getActualAlarmState().get)
          case (None, Some(x)) => if (x.alarm != alarm) notifyAlarm(id,alarmsToTrack(id).getActualAlarmState().get)
        }
      })
    NotificationsSender.msLogger.debug("{} values processed",iasValues.length)
  }
}

object NotificationsSender {
  /** The default time interval (minutes) to send notifications */
  val SendEmailsTimeIntervalDefault: Int = 60*24 // Once a day

  /** The name of the java property to customize the time interval to send notifications */
  val SendEmailsTimeIntervalPropName = "org.eso.ias.emails.timeinterval"

  /** The time of the day to start periodic notifications */
  val StartTimeOfPeriodicNotificationsPropName = "org.eso.ias.emails.starttime"

  /** The default time (UTC) of the day to start periodic notifications */
  val StartTimeOfPeriodicNotificationsDefault = "08:00"

  /** The logger */
  val msLogger: Logger = IASLogger.getLogger(classOf[NotificationsSender])

  /** Build the usage message */
  val cmdLineSyntax: String = "NotificationSender NotificationSender-ID [-h|--help] [-j|-jcdb JSON-CDB-PATH] [-x|--logLevel log level]"

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
    msLogger.info("Params from command line: jcdb={}, logLevel={} supervisor ID={}",
      ret._2.getOrElse("Undefined"),
      ret._3.getOrElse("Undefined"),
      ret._1.getOrElse("Undefined"))
    ret

  }

  /** Launch the emmail sender */
  def main(args: Array[String]): Unit = {
    val parsedArgs = parseCommandLine(args)
    require(parsedArgs._1.nonEmpty, "Missing identifier in command line")

    // The id of the sender
    val emailSenderId = parsedArgs._1.get

    // Get the CDB
    val cdbReader: CdbReader = CdbReaderFactory.getCdbReader(args)
    cdbReader.init()

    logger.debug("Getting the IAS frm the CDB")
    val iasDao: IasDao = {
      val iasOpt = cdbReader.getIas
      if (!iasOpt.isPresent) {
        msLogger.error("Error getting the IAS from the CDB")
        System.exit(-1)
      }
      iasOpt.get()
    }

    // Set the log level
    val actualLogLevel = IASLogger.setLogLevel(
      parsedArgs._3.map(_.toLoggerLogLevel),
      Option(iasDao.getLogLevel).map(_.toLoggerLogLevel),
      None)
    msLogger.info("Log level set to {}",actualLogLevel.getOrElse("default from logback configuration").toString)

    /** The configuration of IASIOs from the CDB */
    logger.debug("Getting the IASIOs from the CDB")
    val iasioDaos: List[IasioDao] = {
      val temp: util.Set[IasioDao] = cdbReader.getIasios.orElseThrow(() => new IllegalArgumentException("IasDaos not found in CDB"))
       CollectionConverters.asScala(temp).toList
    }
    logger.debug("Got {} IASIOs from the CDB",iasioDaos.length)

    logger.debug("Getting templates from the CDB")
    val templateDaos: List[TemplateDao] = {
      val templatesFromCdb =  cdbReader.getTemplates

      if (!templatesFromCdb.isPresent) List.empty[TemplateDao]
      else CollectionConverters.asScala(templatesFromCdb.get()).toList
    }
    logger.debug("Got {} templates from the CDB",templateDaos.length)

    cdbReader.shutdown()
    logger.debug("CdbReader closed")

    val (smtpServer, login,pswd) = {
      val smtpServerOpt = Option(iasDao.getSmtp)
      if (smtpServerOpt.isEmpty) {
        println("ERROR: SMTP not found in the IAS configuration in the CDB")
        System.exit(-2)
      }
      // The string from the CDB
      // The format is user:password@smtpHost
      val smtpConnStr = smtpServerOpt.get
      if (smtpConnStr.count(_=='@')!=1 || smtpConnStr.count(_==':')!=1) {
        msLogger.error(s"SMTP connection string [{}] has wrong format: expected [username]:[password]@smtphost",smtpConnStr)
        System.exit(-2)
      }

      val serverParts = smtpConnStr.split('@')
      if (serverParts.length!=2) {
        msLogger.error(s"SMTP connection string [{}] has wrong format: expected [username]:[password]@smtphost",smtpConnStr)
        System.exit(-3)
      }
      val server=serverParts(1).trim
      val loginPswd=serverParts(0)

      val loginNameParts = loginPswd.split(':')

      if (loginNameParts.length==0) { // No login and no pswd
        (server, None, None)
      } else if (loginNameParts.length==1) { // Only login
        (server, Some(loginNameParts(0).trim), None)
      } else { // login and pswd but login can be empty "" that is an error
        if (loginNameParts(0).isEmpty) {
          msLogger.error(s"SMTP connection string [{}] has wrong format: expected [username]:[password]@smtphost",smtpConnStr)
          System.exit(-4)
        } else {
          (server, Some(loginNameParts(0).trim), Some(loginNameParts(1)))
        }
      }
    }
    msLogger.info("SMTP server {}",smtpServer.asInstanceOf[String])
    val mailSender: Sender = new SmtpSender(
      server = smtpServer.asInstanceOf[String],
      loginName = login.asInstanceOf[Option[String]],
      pswd = pswd.asInstanceOf[Option[String]])


    val valueListener: ValueListener = new NotificationsSender(emailSenderId,mailSender)
    msLogger.debug("NotificationsSender instantiated")


    val kafkaBrokers: Option[String] = {
      val temp = Option(iasDao.getBsdbUrl)
      if (temp.isEmpty) None
      else if (temp.get.isEmpty) None
      else temp
    }

    val kafkaServers = System.getProperties.getProperty(KafkaHelper.BROKERS_PROPNAME,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)



    msLogger.debug("IAS values consumer instantiated")

    val valuesProcessor: IasValueProcessor = new IasValueProcessor(
      emailSenderId,
      List(valueListener),
      kafkaBrokers.get,
      iasDao,
      iasioDaos,
      templateDaos)
    msLogger.debug("IAS values processor instantiated")

    // Start
    msLogger.info("Starting the loop...")
    valuesProcessor.init()
  }
}
