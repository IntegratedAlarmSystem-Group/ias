package org.eso.ias.sink.email

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos.IasTypeDao
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.ValueListener
import org.eso.ias.types.{Alarm, IASTypes, IASValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * The MailSender process the alrsma published in the BSDB and sends mails to registered recipients.
  *
  * To avoid flooding the mail boxes of the recipients, one email is immediately sent when an alarm is
  * initially sent. All the other changes of states are collected and sent all together when a time interval elapses.
  *
  * @param id identifier of the mail
  */
class MailSender(id: String) extends ValueListener(id) {

  /** The logger */
  val msLogger: Logger = IASLogger.getLogger(classOf[MailSender])

  /** The alarms that have a registered recipients to send email */
  val alarmsToTrack:  mutable.Map[String, AlarmStateTracker] = mutable.Map.empty

  /** Maps each recipient with the alarms to notify:
    * one user can be registered to be notified of several alarms
    * The key is the address to send emails to, the value is the list of alarms to notify the recipient
    */
  val alarmsForUser: mutable.Map[String, List[String]] = mutable.Map.empty

  /** The time interval (mins) to send emails */
  val timeIntervalToSendEmails = Integer.getInteger(
    MailSender.sendEmailsTimeIntervalPropName,
    MailSender.sendEmailsTimeIntervalDefault)

  /**
    * Initialization: scans the IasioDaos and prepare data structures for tracking state changes and
    * sending emails
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
  }

  /**
    * Free all the allocated resources
    */
  override protected def close(): Unit = {
    alarmsForUser.clear()
    alarmsToTrack.clear()
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
        alarmsToTrack(value.id)=alarmsToTrack(value.id).stateUpdate(alarm,tStamp)
      })
  }
}

object MailSender {
  /** The default time interval (minutes) to send emails */
  val sendEmailsTimeIntervalDefault: Int = 60

  /** The name of the java property to customize the time interval to send emails */
  val sendEmailsTimeIntervalPropName = "org.eso.ias.emails.timeinterval"
}
