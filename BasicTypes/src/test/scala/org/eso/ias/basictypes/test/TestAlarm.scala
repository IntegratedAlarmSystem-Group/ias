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
  
  it must "start with an nset, unack alarm state" in {
    val alarm: Alarm = Alarm.getInitialAlarmState
    
    assert(!alarm.alarmState.isSet)
    assert(!alarm.isAcked)
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
}