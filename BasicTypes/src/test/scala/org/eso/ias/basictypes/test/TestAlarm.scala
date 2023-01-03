package org.eso.ias.basictypes.test

import org.scalatest.flatspec.AnyFlatSpec
import org.eso.ias.types.Alarm
import org.eso.ias.types.AlarmState
import org.eso.ias.types.Priority

/**
 * Test the Alarm
 */
class TestAlarm extends AnyFlatSpec {
  
  behavior of "An Alarm"
  
  
  it must "not have CLEAR as default" in {
    val alarm: Alarm = new Alarm()
    
    assert(alarm.alarmState.isSet)
  }
  
  /**
   * This test checks if it is possible to increase the priority of any ALARM even
   * of the one with the highest priority
   */
  it must "increase the priority of any alarm" in {
    val maxPrioAl = new Alarm(AlarmState.SET_UNACK, Priority.getMaxPriority)
    assert(maxPrioAl.increasePriority.priority==maxPrioAl.priority)
    
    var alarm = new Alarm(AlarmState.SET_ACK, Priority.getMinPriority)
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
    val minPrioAl = new Alarm(AlarmState.SET_ACK, Priority.getMinPriority)
    assert(minPrioAl.lowerPriority.priority==minPrioAl.priority)
    
    var alarm = new Alarm(AlarmState.SET_ACK, Priority.getMaxPriority)
    for (i <- 0 to Priority.values().length) {
       alarm = alarm.lowerPriority
    }
    assert(alarm.priority==minPrioAl.priority)
  }
}