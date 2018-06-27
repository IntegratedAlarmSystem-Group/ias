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
    * Notify the recipients with the summary of the state changes of the passed alarms
    *
    * @param recipients   the recipients to notify
    * @param alarmStates the states of the alarms to notify
    */
  override def digestNotify(recipients: List[String], alarmStates: List[AlarmStateTracker]): Unit = {
    require(Option(recipients).isDefined && recipients.nonEmpty,"No recipients given")
    require(Option(alarmStates).isDefined && alarmStates.nonEmpty,"No history to send")
  }

  /**
    * Send the notification to notify that an alarm has been set or cleared
    *
    * @param recipients the recipients to notify
    * @param alarmId the ID of the alarm
    * @param alarmState the state to notify
    */
  override def notify(recipients: List[String], alarmId: String, alarmState: AlarmState) = {
    require(Option(recipients).isDefined && recipients.nonEmpty,"No recipients given")
    require(Option(alarmId).isDefined && !alarmId.trim.isEmpty,"Invalid alarm ID")
    require(Option(alarmState).isDefined,"No alarm state to notify")
  }
}
