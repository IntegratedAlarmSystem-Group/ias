package org.eso.ias.sink.email

/**
  * The notification sender sends notification to recipients.
  */
trait Sender {

  /**
    * Notify the recipient of the state changes of the passed alarms
    *
    * @param recipient the recipient to notify
    * @param alarmStates the states of the alarms to notify
    */
  def notify(recipient: String, alarmStates: List[AlarmStateTracker])
}
