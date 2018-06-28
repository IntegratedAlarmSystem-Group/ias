package org.eso.ias.sink.email

import java.time._
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos.IasTypeDao
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.ValueListener
import org.eso.ias.types.{Alarm, IASTypes, IASValue, Validity}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Try}

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

  /** The logger */
  val msLogger: Logger = IASLogger.getLogger(classOf[NotificationsSender])

  /** The alarms that have a registered recipients to send notification */
  val alarmsToTrack:  mutable.Map[String, AlarmStateTracker] = mutable.Map.empty

  /** Maps each recipient with the alarms to notify:
    * one user can be registered to be notified of several alarms
    * The key is the address to send notifications to, the value is the list of alarms to notify to the recipient
    */
  val alarmsForUser: mutable.Map[String, List[String]] = mutable.Map.empty

  /**The executor to send notification: all the instances run in a single thread */
  val executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory {
    override def newThread(runnable: Runnable): Thread = {
      require(Option(runnable).isDefined)
      val threadId = "NotificationSenderThread"
      val ret = new Thread(runnable,threadId)
      ret.setDaemon(true)
      ret
    }
  })

  /** The time interval (mins) to send notifications */
  val timeIntervalToSendEmails: Int = Integer.getInteger(
    NotificationsSender.sendEmailsTimeIntervalPropName,
    NotificationsSender.sendEmailsTimeIntervalDefault)
  msLogger.info("Will send digest emails every {} minutes",timeIntervalToSendEmails)

  val timeTostart: (Int, Int) = {
    val prop = System.getProperty(
      NotificationsSender.startTimeOfPeriodicNotificationsPropName,
      NotificationsSender.startTimeOfPeriodicNotificationsDefault).split(":")
    (Integer.parseInt(prop(0)),Integer.parseInt(prop(1)))
  }
  msLogger.info("First digest at {}:{} UTC",timeTostart._1.toString,timeTostart._2.toString)

  /** Start the periodic tasl to send the digests */
  private def startTimer() = {
    val localNow: ZonedDateTime  = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)

    val nextZone: ZonedDateTime = {
      val temp=localNow.withHour(timeTostart._1).withMinute(timeTostart._2).withSecond(0)
      if(localNow.compareTo(temp) > 0)    temp.plusDays(1)
      else temp
    }

    val duration: Duration = Duration.between(localNow, nextZone);
    val initalDelay = duration.getSeconds
    val timeIntervalSecs = TimeUnit.SECONDS.convert(timeIntervalToSendEmails,TimeUnit.MINUTES)

    executor.scheduleAtFixedRate(new Runnable() {
      override def run(): Unit = {
        msLogger.info("TASK")
        sendDigests()
      }
          },initalDelay,timeIntervalSecs, TimeUnit.SECONDS);
    msLogger.debug("Periodic thread scheduled to run in {} secs every {} secs",initalDelay.toString,timeIntervalSecs)
    msLogger.info("Periodic send of digest scheduled every {} minutes",timeIntervalToSendEmails.toString)
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
    msLogger.info("Tracks and send email notifications for {} alarms",alarmsWithRecipients.size)

    // Saves the ID of the alarms to send notifications in the map to track the changes of states
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
    msLogger.debug("Sending digests")
    alarmsForUser.keys.foreach(user => {
      msLogger.debug("Sending digest of {} alarms to {}",alarmsForUser(user).mkString(","),user)

      val alarmStates: List[AlarmStateTracker] = alarmsForUser(user).map(alarmId => alarmsToTrack(alarmId))
      msLogger.debug("Digests will report the state of {} alarm states for user {}",alarmStates.length,user)
      val sendOp = Try(sender.digestNotify(user,alarmStates))
      if (sendOp.isFailure) {
        msLogger.error("Error sending periodic notification to {}",user, sendOp.asInstanceOf[Failure[_]].exception)
      } else {
        msLogger.debug("Digest sent to {}",user)
      }
    })
    alarmsToTrack.keys.foreach(id => alarmsToTrack(id)=alarmsToTrack(id).reset())
    msLogger.debug("Digests sent to the sender")
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
    val recipients = iasValuesDaos(alarmId).getEmails.split(",")
    msLogger.debug("Sending tonitifcation of alarm {} status change to {}", alarmId, recipients.mkString(","))
    val sendOp = Try(sender.notify(recipients.map(_.trim).toList, alarmId, state))
    if (sendOp.isFailure) msLogger.error("Error sending alarm state notification notification to {}", recipients.mkString(","), sendOp.asInstanceOf[Failure[_]].exception)
  }


  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  override protected def process(iasValues: List[IASValue[_]]): Unit = synchronized {
    msLogger.debug("Processing {} values read from BSDB",iasValues.length)
    // Iterates over non-alarms IASIOs
    val valuesToUpdate = iasValues.filter(v => v.valueType==IASTypes.ALARM && alarmsToTrack.contains(v.id))
      .foreach(value => {
        val alarm = value.asInstanceOf[IASValue[Alarm]].value
        val tStamp: Long = if (value.pluginProductionTStamp.isPresent) {
          value.pluginProductionTStamp.get()
        } else {
          value.dasuProductionTStamp.get()
        }
        val validity = value.iasValidity

        // The last state in the current time interval
        val oldState: Option[AlarmState] = alarmsToTrack(value.id).getActualAlarmState()
        // The state of the last round interval
        val lastRoundState: Option[AlarmState] = alarmsToTrack(value.id).stateOfLastRound

        // Update the state of the alarm state tracker
        alarmsToTrack(value.id) = alarmsToTrack(value.id).stateUpdate(alarm, validity, tStamp)

        (oldState, lastRoundState) match {
          case (None, None) => if (alarm.isSet) notifyAlarm(value.id,alarmsToTrack(value.id).getActualAlarmState().get)
          case (Some(x), _) => if (x.alarm != alarm) notifyAlarm(value.id,alarmsToTrack(value.id).getActualAlarmState().get)
          case (None, Some(x)) => if (x.alarm != alarm) notifyAlarm(value.id,alarmsToTrack(value.id).getActualAlarmState().get)
        }
      })
    msLogger.debug("{} values processed",iasValues.length)
  }
}

object NotificationsSender {
  /** The default time interval (minutes) to send notifications */
  val sendEmailsTimeIntervalDefault: Int = 60*24 // Once a day

  /** The name of the java property to customize the time interval to send notifications */
  val sendEmailsTimeIntervalPropName = "org.eso.ias.emails.timeinterval"

  /** The time of the day to start periodic notifications */
  val startTimeOfPeriodicNotificationsPropName = "org.eso.ias.emails.starttime"

  /** The default time (UTC) of the day to start periodic notifications */
  val startTimeOfPeriodicNotificationsDefault = "08:00"
}
