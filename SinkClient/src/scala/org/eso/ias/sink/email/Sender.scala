package org.eso.ias.sink.email

/**
  * The notification sender sends notification to recipients.
  */
trait Sender {

  /**
    * Notify the recipients of the state changes of the passed alarms
    *
    * @param recipient the recipient to notify
    * @param alarmStates the states of the alarms to notify
    */
  def digestNotify(recipient: String, alarmStates: List[AlarmStateTracker])

  /**
    * Send the notification to notify that an alarm has been set or cleared
    *
    * @param recipients the recipients to notify
    * @param alarmId the ID of the alarm
    * @param alarmState the state to notify
    */
  def notify(recipients: List[String], alarmId: String, alarmState: AlarmState)
}
