package org.eso.ias.sink.email

/**
  * The sender of notifications by emails.
  *
  * @param server The SMTP server to send notifications to the receipients
  * @param loginName the optional login name to pass to the server
  * @param pswd the optional passowrd to pass to the server
  */
class SmtpSender(val server: String, val loginName: Option[String], val pswd: Option[String]) extends Sender {
  /**
    * Notify the recipient of the state changes of the passed alarms
    *
    * @param recipient   the recipient to notify
    * @param alarmStates the states of the alarms to notify
    */
  override def notify(recipient: String, alarmStates: List[AlarmStateTracker]): Unit = {}
}
