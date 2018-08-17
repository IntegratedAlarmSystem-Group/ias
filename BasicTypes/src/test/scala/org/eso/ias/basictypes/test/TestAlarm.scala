package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.types.Alarm

/**
 * Test the Alarm
 */
class TestAlarm extends FlatSpec {
  
  behavior of "An Alarm"
  
  
  it must "not have CLEAR as default" in {
    val alarm: Alarm = Alarm.getSetDefault
    
    assert(alarm!=Alarm.CLEARED)
  }
  
  it must "forbid to increase the priority of a CLEARED" in {
    val alarm = Alarm.CLEARED
    assertThrows[IllegalStateException] {
      alarm.increasePriority()
    }
  }
  
  it must "forbid to lower the priority of a CLEARED" in {
    val alarm = Alarm.CLEARED
    assertThrows[IllegalStateException] {
      alarm.lowerPriority()
    }
  }
  
  /**
   * This test checks if it is possible to increase the priority of any ALARM even
   * of the one with the highest priority
   */
  it must "increase the priority of any alarm" in {
    val maxPrioAl = Alarm.getMaxPriorityAlarm
    assert(maxPrioAl.increasePriority==maxPrioAl)
    
    var alarm = Alarm.getMinPriorityAlarm
    for (i <- 0 to Alarm.values().length) {
       alarm = alarm.increasePriority
    }
    assert(maxPrioAl==alarm)
    
  }
  
  /**
   * This test checks if it is possible to increase the priority of any ALARM even
   * of the one with the highest priority
   */
  it must "lower the priority of any alarm" in {
    val minPrioAl = Alarm.getMinPriorityAlarm
    assert(minPrioAl.lowerPriority==minPrioAl)
    
    var alarm = Alarm.getMaxPriorityAlarm
    for (i <- 0 to Alarm.values().length) {
       alarm = alarm.lowerPriority
    }
    assert(alarm==minPrioAl)
  }
  
  it must "return the proper alarm from the priority" in {
    Alarm.values().foreach( alarm => {
      if (alarm.priorityLevel.isPresent) {
        val priority = alarm.priorityLevel.get()
        val alarmFromPrio = Alarm.fromPriority(priority)
        assert(alarmFromPrio==alarm)
      }
    })
    
    assertThrows[IllegalArgumentException]{ Alarm.fromPriority(0) }
    assertThrows[IllegalArgumentException]{ Alarm.fromPriority(-1) }
    assertThrows[IllegalArgumentException]{ Alarm.fromPriority(120) }
  }
  
}