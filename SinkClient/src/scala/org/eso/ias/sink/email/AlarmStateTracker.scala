package org.eso.ias.sink.email

import org.eso.ias.types.{Alarm, IasValidity}

/**
  * The state of the alarm recorded by the tracker.
  *
  * @param alarm the activation state of the alarm
  * @param validity the validity
  * @param timestamp the timestamp when the alarm has been set or cleared
  */
class AlarmState(val alarm: Alarm, val validity: IasValidity, val timestamp: Long)

/**
  * AlarmStateTracker records the changes of the state of an alarm to be notified
  * to the user by email or some other mechanism.
  *
  * The update of an alarm is a AlarmState.
  * The state is one of the values defined in Alarm so it records acrtivation, priority, validity and deactivation.
  *
  * The AlarmStateTracker is immutable.
  * New change of state are added at the head of the list.
  *
  * @param id the identifier of the alarm
  * @param stateChanges The state changes recorded so far
  */
class AlarmStateTracker private(
                 val id: String,
                 val stateChanges: List[AlarmState]) {
  require(Option(id).isDefined && !id.isEmpty)

  /**
    * Build a new AlarmStateTracker that records the new alarm change at a given time.
    *
    * @param alarm The new alarm
    * @param validity the validity
    * @param timestamp The timestamp when the alarm has been produced
    * @return the AlarmStateTracker that records this change
    */
  def stateUpdate(alarm: Alarm, validity: IasValidity, timestamp: Long): AlarmStateTracker = {
    require(Option(alarm).isDefined,"Invalid empty alarm")
    require(Option(validity).isDefined,"Invalid empty validity")

    val state = new AlarmState(alarm,validity,timestamp)
    (stateChanges) match {
      case Nil =>  new AlarmStateTracker(id,List(state))
      case x::rest => if (x.alarm!=alarm || x.validity!=validity) new AlarmStateTracker(id,state::stateChanges)
                      else this
    }


    new AlarmStateTracker(id,state::stateChanges)
  }

  /**
    * Reset the AlarmStateTracker to be ready to record the changes of states
    * of the next time interval
    *
    * @return The AlarmStateTracker to record changes during the next time interval
    */
  def reset(): AlarmStateTracker = {
    stateChanges match {
      case Nil => this
      case _ => new AlarmStateTracker(id,Nil)
    }
  }

  /**
    * @return the number of changes of states recorded
    */
  def numOfChanges: Int = stateChanges.length

  /**
    * @return The actual state, if exists
    */
  def getActualAlarmState(): Option[AlarmState] = {
    if(stateChanges.isEmpty) None
    else Some(stateChanges.head)
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
