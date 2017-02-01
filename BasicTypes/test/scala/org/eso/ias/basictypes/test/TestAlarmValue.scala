package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.input.AlarmState
import org.eso.ias.prototype.input.InvalidStateTransitionException
import org.eso.ias.prototype.input.Set
import org.eso.ias.prototype.input.Clear
import org.eso.ias.prototype.input.AckState

/**
 * Test the AlarmValue State Machine
 */
class TestAlarmValue extends FlatSpec {
   
   "The AlarmValue" must "initially have a Unknown state" in {
     val v = new AlarmValue()
     assert(v.alarmState == AlarmState.Unknown)
   }
   
   it must "initially be unshelved and not acknowledged" in {
     val v = new AlarmValue()
     assert(!v.shelved)
     assert(v.acknowledgement == AckState.Acknowledged)
   }
   
   /**
    * Test all transition from a Unknown alarm state
    */
   "Unknown state" must "correctly handle events" in {
     val as = new AlarmValue()
     
     val setAlState = AlarmValue.transition(as,Set())
     setAlState.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Active))
     
     val clearedAS = AlarmValue.transition(as,Clear())
     clearedAS.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Cleared))
   }
   
   /**
    * Test all transition from a Active alarm state
    */
   "Active state" must "correctly handle events" in {
     
     // Setup: generate an alarm and transition to the ActiveAndNew state 
     val newas = new AlarmValue()
     val activeAndNewAS = AlarmValue.transition(newas, Set())
     activeAndNewAS.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Active))
     
     // Check event handling from ActiveAndNew state
     val afterSet = AlarmValue.transition(activeAndNewAS.right.get,Set())
     afterSet.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Active))
     
     val afterClear = AlarmValue.transition(activeAndNewAS.right.get,Clear())
     afterClear.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Cleared))
   }
   
   /**
    * Test all transition from a Cleared alarm state
    */
   "Cleared state" must "correctly handle events" in {
     
     // Setup: generate an alarm and transition to the ActiveAndAcknowledged state 
     val newas = new AlarmValue()
     val activeAndNewAS = AlarmValue.transition(newas, Set())
     val clearedAS = AlarmValue.transition(activeAndNewAS.right.get, Clear())
     clearedAS.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Cleared))
     
     // Check event handling from ActiveAndAcknowledged state
     val afterSet = AlarmValue.transition(clearedAS.right.get,Set())
     afterSet.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Active))
     
     val afterClear = AlarmValue.transition(clearedAS.right.get,Clear())
     afterClear.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Cleared))
   }
   
   "New Alarm" must "remain New after transitions" in {
     val newas = new AlarmValue()
     val activeAS = AlarmValue.transition(newas, Set())
     activeAS.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Active))
     assert(activeAS.right.get.acknowledgement == AckState.New)
     
     assert(AlarmValue.transition(activeAS.right.get,Set()).right.get.acknowledgement == AckState.New)
     assert(AlarmValue.transition(activeAS.right.get,Clear()).right.get.acknowledgement == AckState.New)
   }
   
   "Acknowledged Alarm" must "remain Acknowledged after transitions" in {
     val newas = new AlarmValue()
     val activeAS = AlarmValue.transition(newas, Set())
     activeAS.fold( (ex) => throw ex, (v) =>assert(v.alarmState == AlarmState.Active))
     assert(activeAS.right.get.acknowledgement == AckState.New)
     
     val ackedAS = activeAS.right.get.acknowledge()
     assert(ackedAS.acknowledgement == AckState.Acknowledged)
     
     assert(AlarmValue.transition(activeAS.right.get,Set()).right.get.acknowledgement == AckState.New)
     assert(AlarmValue.transition(activeAS.right.get,Clear()).right.get.acknowledgement == AckState.New)
   }
}