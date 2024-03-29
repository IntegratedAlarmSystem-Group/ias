package org.eso.ias.sink.test

import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.email.AlarmStateTracker
import org.eso.ias.types.{Alarm, IasValidity, Priority, Validity}
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test the AlarmStateTracker
  */
class AlarmStateTrackerTest extends AnyFlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(classOf[AlarmStateTrackerTest])

  val states: List[Alarm] = List(
    Alarm.getInitialAlarmState(Priority.LOW).set(),
    Alarm.getInitialAlarmState(Priority.MEDIUM).set(),
    Alarm.getInitialAlarmState(Priority.HIGH).set(),
    Alarm.getInitialAlarmState(Priority.CRITICAL).set(),
    Alarm.getInitialAlarmState)

  val validities = List(IasValidity.RELIABLE, IasValidity.UNRELIABLE)

  behavior of "The alarm states tracker"

  it must "save the id" in {
    val v = AlarmStateTracker(id = "AlarmID")
    assert(v.id == "AlarmID")
  }

  it must "have an empy history at begining" in {
    val v = AlarmStateTracker(id = "AlarmID")
    assert(v.stateChanges.isEmpty)
  }

  it must "have an empy last state at begining" in {
    val v = AlarmStateTracker(id = "AlarmID")
    assert(v.stateOfLastRound.isEmpty)
  }

  it must "save all the changes (different values)" in {
    var v = AlarmStateTracker(id = "AlarmID")

    for {
      state <- states
      validity <- validities
    } {
      v = v.stateUpdate(state, validity, System.currentTimeMillis())
      Thread.sleep(25) // Have different timestamps
    }

    assert(v.stateChanges.length == states.length * validities.length)
    assert(v.getActualAlarmState().isDefined)
  }

  it must "return the last alarm state" in {
    var v = AlarmStateTracker(id = "AlarmID")
    assert(v.getActualAlarmState().isEmpty)

    for {
      state <- states
      validity <- validities
    } {
      v = v.stateUpdate(state, validity, System.currentTimeMillis())
      Thread.sleep(25) // Have different timestamps
    }
    v = v.stateUpdate(Alarm.getInitialAlarmState(Priority.MEDIUM).set(), IasValidity.RELIABLE, 1024)
    val lastState = v.getActualAlarmState()
    assert(lastState.isDefined)
    assert(lastState.get.alarm.isSet)
    assert(lastState.get.alarm.priority==Priority.MEDIUM)
    assert(lastState.get.validity==IasValidity.RELIABLE)
    assert(lastState.get.timestamp==1024)
  }

  it must "clear the history after a reset and save the last SET state" in {
    var v = AlarmStateTracker(id = "AlarmID")

    for {
      state <- states
      validity <- validities
    } {
      v = v.stateUpdate(state, validity, System.currentTimeMillis())
      Thread.sleep(25) // Have different timestamps
    }
    v = v.stateUpdate(Alarm.getInitialAlarmState(Priority.MEDIUM).set(), IasValidity.RELIABLE, 1024)
    v = v.reset()
    assert(v.stateChanges.isEmpty)
    assert(v.getActualAlarmState().isEmpty)
    assert(v.stateOfLastRound.isDefined)
    assert(v.stateOfLastRound.get.alarm.isSet)
    assert(v.stateOfLastRound.get.alarm.priority==Priority.MEDIUM)
    assert(v.stateOfLastRound.get.validity==IasValidity.RELIABLE)
    assert(v.stateOfLastRound.get.timestamp==1024)
  }

  it must "clear the history after a reset and not save the last CLEARED state" in {
    var v = AlarmStateTracker(id = "AlarmID")

    for {
      state <- states
      validity <- validities
    } {
      v = v.stateUpdate(state, validity, System.currentTimeMillis())
      Thread.sleep(25) // Have different timestamps
    }
    v = v.stateUpdate(Alarm.getInitialAlarmState, IasValidity.RELIABLE, 1024)
    v = v.reset()
    assert(v.stateChanges.isEmpty)
    assert(v.getActualAlarmState().isEmpty)
    assert(v.stateOfLastRound.isDefined)
  }

  it must "not save a CLEARED if empty" in {
    var v = AlarmStateTracker(id = "AlarmID")
    v = v.stateUpdate(Alarm.getInitialAlarmState, IasValidity.RELIABLE, System.currentTimeMillis())
    v = v.stateUpdate(Alarm.getInitialAlarmState, IasValidity.UNRELIABLE, System.currentTimeMillis())

    assert(v.stateChanges.isEmpty)
    assert(v.stateOfLastRound.isEmpty)

  }

  it must "not save twice the same state" in {
    var v = AlarmStateTracker(id = "AlarmID")
    val a = Alarm.getInitialAlarmState(Priority.HIGH).set()
    v = v.stateUpdate(a, IasValidity.RELIABLE, System.currentTimeMillis())
    v = v.stateUpdate(a, IasValidity.RELIABLE, System.currentTimeMillis())

    assert(v.stateChanges.length==1)
    assert(v.stateOfLastRound.isEmpty)

    v = v.stateUpdate(a, IasValidity.UNRELIABLE, System.currentTimeMillis())
    assert(v.stateChanges.length==2)
    assert(v.stateOfLastRound.isEmpty)
  }

  it must "not a save a state equals to the stateOfLastRound" in {
    var v = AlarmStateTracker(id = "AlarmID")
    val a = Alarm.getInitialAlarmState(Priority.HIGH).set()
    v = v.stateUpdate(a, IasValidity.RELIABLE, System.currentTimeMillis())
    v = v.reset()

    assert(v.stateChanges.isEmpty)
    assert(v.stateOfLastRound.isDefined)

    v = v.stateUpdate(a, IasValidity.RELIABLE, System.currentTimeMillis())
    v = v.stateUpdate(a, IasValidity.RELIABLE, System.currentTimeMillis())
    v = v.stateUpdate(a, IasValidity.RELIABLE, System.currentTimeMillis())
    assert(v.stateChanges.isEmpty)
    assert(v.stateOfLastRound.isDefined)

    v = v.stateUpdate(a, IasValidity.UNRELIABLE, System.currentTimeMillis())
    assert(v.stateChanges.length==1)
    assert(v.stateOfLastRound.isDefined)

  }






}
