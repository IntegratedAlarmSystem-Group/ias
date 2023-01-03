package org.eso.ias.basictypes.test

import org.scalatest.flatspec.AnyFlatSpec
import org.eso.ias.types.Alarm
import org.eso.ias.types.AlarmState
import org.eso.ias.types.Priority

/**
 * Test the Alarm including the alarm state machine
 */
class TestAlarm extends AnyFlatSpec {
  
  behavior of "The alarm state"

  /** Test the initial state */
  it must "start with an unset, ack alarm state" in {
    val alarm: Alarm = Alarm.getInitialAlarmState
    
    assert(!alarm.alarmState.isSet)
    assert(alarm.isAcked)
  }

  /** Checks if applying methods changes the states in the proper way */
  it must "correctly implement the state transitions" in {
    val startAlarmState = Alarm.getInitialAlarmState
    assert(!startAlarmState.isSet)
    assert(startAlarmState.isAcked)

    // Set the initial alarm
    val setAlarm = startAlarmState.set()
    assert(setAlarm.isSet)
    assert(!setAlarm.isAcked)

    val unsetAlarm = setAlarm.clear()
    assert(!unsetAlarm.isSet)
    assert(!unsetAlarm.isAcked)

    val ackUnsetAlm = unsetAlarm.ack()
    assert(ackUnsetAlm.isAcked)
    assert(!ackUnsetAlm.isSet)
    assert(ackUnsetAlm==startAlarmState)

    val setAckAlarm = setAlarm.ack()
    assert(setAckAlarm.isAcked)
    assert(setAckAlarm.isSet)

    assert(setAckAlarm.clear()==startAlarmState)

    assert(unsetAlarm.set()==setAlarm)
  }

  /** It checks that acknowledging an acked alarm produces an acked alarm */
  it must "not change the state of ACKed alarms when an ACK is issued" in {
    val startAlarmState = Alarm.getInitialAlarmState
    assert(startAlarmState.isAcked)

    assert(startAlarmState.ack()==startAlarmState)

    val setAlarm = startAlarmState.set()
    val setAckAlarm = setAlarm.ack()
    assert(setAckAlarm.ack()==setAckAlarm)
  }

  /** It checks that setting an alarm that is already set produces an alarm that is set */
  it must "not change the state of set alarms when an SET is issued" in {
    val startAlarmState = Alarm.getInitialAlarmState
    val setAlarm = startAlarmState.set()
    assert(setAlarm.set()==setAlarm)

    val setAckAlarm = setAlarm.ack()
    assert(setAckAlarm.set()==setAckAlarm)

  }

  /** It checks that clearing an alarm that is already clear produces an alarm that is clear */
  it must "not change the state of cleared alarms when an CLEAR is issued" in {
    val startAlarmState = Alarm.getInitialAlarmState
    assert(startAlarmState.clear()==startAlarmState)

    val setAlarm = startAlarmState.set()
    val unsetAlarm = setAlarm.clear()
    assert(unsetAlarm.clear()==unsetAlarm)

  }

  behavior of "An Alarm"
  
  /**
   * This test checks if it is possible to increase the priority of any ALARM even
   * of the one with the highest priority
   */
  it must "increase the priority of any alarm" in {
    val maxPrioAl = Alarm.getInitialAlarmState(Priority.getMaxPriority)
    assert(maxPrioAl.increasePriority.priority==maxPrioAl.priority)
    
    var alarm = Alarm.getInitialAlarmState(Priority.getMinPriority)
    for (i <- 0 to Priority.values().length) {
       alarm = alarm.increasePriority
    }
    assert(maxPrioAl.priority==alarm.priority)
    
  }
  
  /**
   * This test checks if it is possible to lower the priority of any ALARM even
   * of the one with the highest priority
   */
  it must "lower the priority of any alarm" in {
    val minPrioAl = Alarm.getInitialAlarmState(Priority.getMinPriority).set()
    assert(minPrioAl.lowerPriority.priority==minPrioAl.priority)
    
    var alarm = Alarm.getInitialAlarmState(Priority.getMaxPriority).set()
    for (i <- 0 to Priority.values().length) {
       alarm = alarm.lowerPriority
    }
    assert(alarm.priority==minPrioAl.priority)
  }

  it must "assign a given priority" in {
    val alarm = Alarm.getInitialAlarmState(Priority.CRITICAL)
    assert(alarm.priority==Priority.CRITICAL)

    assert(alarm.setPriority(Priority.LOW).priority==Priority.LOW)
  }
}