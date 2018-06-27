package org.eso.ias.sink.email

import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos.IasTypeDao
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.ValueListener
import org.eso.ias.types.{Alarm, IASTypes, IASValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * The NotificationsSender process the alarms published in the BSDB and sends mails to registered recipients.
  *
  * To avoid flooding the mail boxes of the recipients, one notification is immediately sent when an alarm is
  * initially SET. All the other changes of states are collected and sent all together when a time interval elapses.
  *
  * To keep the heuristic simple, in one time interval one notification is sent
  * - when the alarm state changes to set for the first time
  * - after the time interval elapses: the notification summarizes the changes of states recorded in the time interval
  * - if the alarm was set at the beginning of the time interval and its state did not change, the NotificationsSender
  * does not send any notification
  *
  * In general, a condition to send a notification is when the alarm si set for the first time and,
  * after the time interval elapses, and the number of recorded changes is greater then 1.
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
      override def run(): Unit = ???
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
    * Periodically sends the notifications (summary) of the changes of stats
    * of th emonitored alarms during the time interval
    */
  def periodicNotification(): Unit = synchronized {

  }

  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  override protected def process(iasValues: List[IASValue[_]]): Unit = {
    // Iterates over non-alarms IASIOs
    val valuesToUpdate = iasValues.filter(v => v.valueType==IASTypes.ALARM && alarmsToTrack.contains(v.id))
      .foreach(value => {
        val alarm = value.asInstanceOf[IASValue[Alarm]].value
        val tStamp: Long = if (value.pluginProductionTStamp.isPresent) {
          value.pluginProductionTStamp.get()
        } else {
          value.dasuProductionTStamp.get()
        }
        // Update the state of the alarm state tracker
        val newStatesTracker=alarmsToTrack(value.id).stateUpdate(alarm,tStamp)
        alarmsToTrack(value.id)=newStatesTracker
      })
  }
}

object NotificationsSender {
  /** The default time interval (minutes) to send notifications */
  val sendEmailsTimeIntervalDefault: Int = 60

  /** The name of the java property to customize the time interval to send notifications */
  val sendEmailsTimeIntervalPropName = "org.eso.ias.emails.timeinterval"
}
