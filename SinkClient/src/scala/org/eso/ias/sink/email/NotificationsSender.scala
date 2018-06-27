package org.eso.ias.sink.email

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

  /**
    * The executor to send notification: all the instances run in a single thread
    */
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

    // Start the periodic notification
    executor.scheduleWithFixedDelay(new Runnable {
      override def run(): Unit = periodicNotification()
    }, timeIntervalToSendEmails, timeIntervalToSendEmails, TimeUnit.MINUTES)
    msLogger.info("Periodic send of notifications every {} minutes",timeIntervalToSendEmails.toString)
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
  def periodicNotification(): Unit = synchronized {
    // Send one email to each user
    //
    // The email contains the summary of all the alarsm about which the user wants to get notifications
    alarmsForUser.keys.foreach(user => {
      logger.debug("Sending digest of {} alarms to {}",alarmsForUser(user).mkString(","),user)
      val alarmStates = alarmsForUser(user).map(alarmId => alarmsToTrack(id))
      val sendOp = Try(sender.digestNotify(List(user),alarmStates))
      if (sendOp.isFailure) {
        logger.error("Error sending periodic notification to {}",user, sendOp.asInstanceOf[Failure[_]].exception)
      }
    })
    alarmsToTrack.keys.foreach(id => alarmsToTrack(id)=alarmsToTrack(id).reset())
  }


  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  override protected def process(iasValues: List[IASValue[_]]): Unit = synchronized {
    // Iterates over non-alarms IASIOs
    val valuesToUpdate = iasValues.filter(v => v.valueType==IASTypes.ALARM && alarmsToTrack.contains(v.id))
      .foreach(value => {
        val alarm = value.asInstanceOf[IASValue[Alarm]].value
        val tStamp: Long = if (value.pluginProductionTStamp.isPresent) {
          value.pluginProductionTStamp.get()
        } else {
          value.dasuProductionTStamp.get()
        }
        val validity = Validity(value.iasValidity)
        val oldState: Option[AlarmState] = alarmsToTrack(value.id).getActualAlarmState()

        // Update the state of the alarm state tracker
        alarmsToTrack(value.id)=alarmsToTrack(value.id).stateUpdate(alarm,validity, tStamp)

        // Send notification
        val recipients: Array[String] = iasValuesDaos(value.id).getEmails.split(",")

        // Send immediate notification if the new alarm is different from the previous one
        // i..e the activation or the priority changed
        if (oldState.isEmpty || oldState.get.alarm!=alarm) {
          val recipients = iasValuesDaos(value.id).getEmails.split(",")
          logger.debug("Sending tonitifcation of alarm {} status change to {}",value.id, recipients.mkString(","))
          alarmsToTrack(value.id).getActualAlarmState().foreach( state => {
             val sendOp = Try(sender.notify(recipients.map(_.trim).toList,value.id,state))
            if (sendOp.isFailure) logger.error("Error sending alarm state notification notification to {}",recipients.mkString(","), sendOp.asInstanceOf[Failure[_]].exception)
          })
        }
      })
  }
}

object NotificationsSender {
  /** The default time interval (minutes) to send notifications */
  val sendEmailsTimeIntervalDefault: Int = 60

  /** The name of the java property to customize the time interval to send notifications */
  val sendEmailsTimeIntervalPropName = "org.eso.ias.emails.timeinterval"
}
