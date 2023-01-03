'''
Created on May 10, 2018

@author: acaproni
'''

from IasBasicTypes.Priority import Priority
from IasBasicTypes.AlarmState import AlarmState

class Alarm():
    '''
    The alarm as defined in org.eso.ias.types.Alarm
    '''


    def __init__(self, alarmState, priority):
        '''
        Constructor

        The constructor should  be used only from functions of this class.

        Alarms should be manipulated starting from the alarm representing the
        initial state of the state machine and calling modifiers.

        The alarm is immutable

        :param alarmState: the state
        :param priority: the priority
        '''
        if alarmState is None or priority is None:
            raise ValueError("Invalid None priority or alarm state")

        self.alarmState = alarmState
        self.priority = priority

    def __eq__(self, other):
        '''
        Redefine equality
        :param other: the objecty to compare
        :return: True if the state and priority of this alarm matches with the one of the other;
                 False otherwise
        '''
        if other is None:
            return False
        elif isinstance(other, Alarm):
            return self.alarmState==other.alarmState and self.priority==other.priority
        else:
            return False

    @staticmethod
    def get_initial_alarmstate(priority=Priority.get_default_priority()):
        '''
        :param priority: the priority of the initial alarm
        :return: the initial alarm state
        '''
        return Alarm(AlarmState.CLEAR_ACK, priority)

    def ack(self):
        '''
        Acknowledge the alarm
        :return: a new alarm with the acknowledgment
        '''
        newState = self.alarmState.ack()
        if self.alarmState==newState:
            return self
        else:
            return Alarm(newState, self.priority)

    def set(self):
        '''
        Set the alarm
        :return: a new set alarm
        '''
        newState = self.alarmState.set()
        if self.alarmState==newState:
            return self
        else:
            return Alarm(newState, self.priority)

    def clear(self):
        '''
        Clear the alarm
        :return: a new cleared alarm
        '''
        newState = self.alarmState.clear()
        if self.alarmState==newState:
            return self
        else:
            return Alarm(newState, self.priority)

    def set_if(self, condition):
        if condition:
            return self.set()
        else:
            return self.clear()

    def is_set(self):
        '''
        :return: True if the alarm is SET; False otherwise
        '''
        return self.alarmState.is_set()

    def is_acked(self):
        '''
        :return: True if the alarm is ACKnowledged; False otherwise
        '''
        return self.alarmState.is_acked()

    def increase_priority(self):
        """
        :return: an alarm with the increased priority
        """
        return Alarm(self.alarmState, self.priority.get_higher_priority())

    def lower_priority(self):
        """
        :return: an alarm with lowered priority
        """
        return Alarm(self.alarmState, self.priority.get_lower_priority())

    def set_priority(self, priority):
        '''
        :param priority: the priority to set
        :return: the alarm with the new priority
        '''
        return Alarm(self.alarmState, priority)

    def to_string(self):
        """
        :return: the string representation of the alarm
        """
        return f"{self.alarmState.to_string()}:{self.priority.to_string()}"

    @staticmethod
    def fromString(alarmString):
        '''
        :param alarmString the string representation of an Alarm like
                      Alarm.SET_CRITICAL or SET_MEDIUM
        :return the alarm represented by the passed a string
        '''
        if alarmString is None or alarmString=="":
            raise ValueError("Invalid string representation of an alarmString")

        temp = str(alarmString)
        parts = alarmString.split(":")
        if len(parts)!=2:
            raise ValueError(f"Malformed alarm string: [$alarmString]")

        alState = AlarmState.value_of(parts[0])
        prio = Priority.value_of(parts[1])

        return Alarm(alState, prio)
