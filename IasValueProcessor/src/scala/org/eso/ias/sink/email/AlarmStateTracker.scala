package org.eso.ias.sink.email

import org.eso.ias.types.Alarm

/**
  * AlarmStateTracker records the changes of the state of an alarm to be notified
  * to the user by email.
  *
  * The update of an alarm is a tuple composed of the alarm and the timestamp of the change.
  *
  * The AlarmStateTracker records the change of states of an alarms that means that
  * one update is accepted if the state of the alarm is different from the last recorded state.
  * When the owner wants to send an email, it reset the AlarmStateTracker to let it ready to record the changes
  * of the states in the next time interval.
  *
  * The AlarmStateTracker is immutable.
  * New change of state are added at the head of the list.
  *
  * @param id the identifier of the alarm
  * @param stateChanges The state changes recorded so far
  */
class AlarmStateTracker private(
                 id: String,
                 val stateChanges: List[Tuple2[Alarm, Long]]) {
  require(Option(id).isDefined && !id.isEmpty)

  /**
    * Build a new AlarmStateTracker that records the new alarm change at a given time.
    *
    * @param alarm The new alarms
    * @param timestamp The timestamp when the alarm has been produced
    * @return the AlarmStateTracker that records this change
    */
  def stateUpdate(alarm: Alarm, timestamp: Long): AlarmStateTracker = {
    require(Option(alarm).isDefined,"Invalid empty alarm")

    (stateChanges,alarm) match {
      case (Nil, Alarm.CLEARED) => this
      case (Nil, _) => new AlarmStateTracker(id,(alarm,timestamp)::Nil)
      case (x::rest, alarm) =>
        if (x._1==alarm) {
          this
        } else {
          new AlarmStateTracker(id,(alarm,timestamp)::stateChanges)
        }
    }
  }

  /**
    * Reset the AlarmStateTracker to be ready to record the next changes of states
    * of the next time interval
    *
    * @return The AlarmStateTracker to record changes during the next time interval
    */
  def reset(): AlarmStateTracker = {
    stateChanges match {
      case Nil => this
      case x::rest => new AlarmStateTracker(id,List(x))
    }
  }

}

object AlarmStateTracker {

  /**
    * Bild a new AlarmStateTracker with the given id
    *
    * @param id The identifier of the alarm
    * @return The alarm state changes tracking
    */
  def apply(id: String): AlarmStateTracker = new AlarmStateTracker(id,Nil)

}
