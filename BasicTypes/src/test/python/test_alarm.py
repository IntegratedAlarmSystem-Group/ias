#! /usr/bin/env python3

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.AlarmState import AlarmState
from IasBasicTypes.Priority import Priority

class TestAlarm():

    def test_initial_alarm(self):
        alarm = Alarm.get_initial_alarmstate()
        assert(alarm.alarmState==AlarmState.CLEAR_ACK)
        assert(alarm.priority==Priority.get_default_priority())
        assert(not alarm.is_set())
        assert(alarm.is_acked())

    def test_marshalling(self):
        alarm = Alarm.get_initial_alarmstate()
        alStr = alarm.to_string()
        al2 = Alarm.fromString(alStr)
        assert(alarm.alarmState==al2.alarmState)
        assert(alarm.priority==al2.priority)

    def test_assign_priority(self):
        alarm = Alarm.get_initial_alarmstate()
        al2 = alarm.set_priority(Priority.CRITICAL)
        assert(al2.priority==Priority.CRITICAL)

        al3 = Alarm.get_initial_alarmstate(Priority.LOW)
        assert(al3.priority==Priority.LOW)

    def test_inc_priority(self):
        alarm = Alarm.get_initial_alarmstate(Priority.HIGH)
        al2 = alarm.increase_priority()
        assert(al2.priority==Priority.CRITICAL)
        al3 = al2.increase_priority()
        assert(al3.priority==Priority.CRITICAL)

    def test_dec_priority(self):
        alarm = Alarm.get_initial_alarmstate(Priority.MEDIUM)
        al2 = alarm.lower_priority()
        assert(al2.priority==Priority.LOW)
        al3 = al2.lower_priority()
        assert(al3.priority==Priority.LOW)

    def test_clear_a_cleared_alarm(self):
        alarm = Alarm.get_initial_alarmstate()
        assert(not alarm.is_set())
        al2 = alarm.clear()
        assert(not al2.is_set())

    def test_set_a_set_alarm(self):
        alarm = Alarm.get_initial_alarmstate()
        assert(not alarm.is_set())
        al2 = alarm.set()
        assert(al2.is_set())
        al3 = al2.set()
        assert(al3.is_set())

    def test_state_transitions(self):
        startAlarmState = Alarm.get_initial_alarmstate()
        assert(not startAlarmState.is_set())
        assert(startAlarmState.is_acked())

        # Set the initial alarm
        setAlarm = startAlarmState.set()
        assert(setAlarm.is_set())
        assert(not setAlarm.is_acked())

        unsetAlarm = setAlarm.clear()
        assert(not unsetAlarm.is_set())
        assert(not unsetAlarm.is_acked())

        ackUnsetAlm = unsetAlarm.ack()
        assert(ackUnsetAlm.is_acked())
        assert(not ackUnsetAlm.is_set())
        assert(ackUnsetAlm.alarmState==startAlarmState.alarmState)

        setAckAlarm = setAlarm.ack()
        assert(setAckAlarm.is_acked())
        assert(setAckAlarm.is_set())

        assert(setAckAlarm.clear()==startAlarmState)

        assert(unsetAlarm.set()==setAlarm)
