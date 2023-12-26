'''
The alarm state and state machine of alarms as defined in org.eso.ias.types.AlarmState
'''

from enum import Enum

class AlarmState(Enum):

    # Alarm clear and acknowledged
    CLEAR_ACK = 0

    # Alarm clear and unacknowledged
    CLEAR_UNACK = 1

    # Alarm set and unacknowledged
    SET_UNACK = 2

    # Alarm set and acknowledged
    SET_ACK = 3

    def is_set(self):
        '''
        :return True if the alarm is set; False otherwise
        '''
        return self==AlarmState.SET_ACK or self==AlarmState.SET_UNACK

    def is_acked(self):
        '''
        :return True if the alarm is acknowledged; False otherwise
        '''
        return self==AlarmState.SET_ACK or self==AlarmState.CLEAR_ACK

    def ack(self):
        '''
        The alarm has been acknowledged (by the operator)
        :return:
        '''
        if self==AlarmState.SET_ACK or self==AlarmState.CLEAR_ACK:
            return self
        elif self==AlarmState.SET_UNACK:
            return AlarmState.SET_ACK
        else: # CLEAR_UNACK
            return AlarmState.CLEAR_ACK

    def set(self):
        '''
        The alarm has been set (by the TF)
        :return:
        '''
        if self==AlarmState.SET_ACK or self==AlarmState.SET_UNACK:
            return self
        else:
            return AlarmState.SET_UNACK

    def clear(self):
        if self==AlarmState.CLEAR_ACK or self==AlarmState.CLEAR_UNACK:
            return self
        elif self==AlarmState.SET_ACK:
            return AlarmState.CLEAR_ACK
        else: #SET_UNACK
            return AlarmState.CLEAR_UNACK

    @staticmethod
    def value_of(name):
        '''
        Get and return the AlarmState from the passed name.

        It delegates to Enum.__getitem__(cls, name)
        :param cls:  the class
        :param name: the name of the alarm state
        :return: the state with the given name
        :raise KeyError if the name does not correspond to a priority
        '''
        return AlarmState.__getitem__(name)

    def to_string(self):
        '''

        :param cls: the class
        :return: the string representation of the alarm state
        '''
        return self.name

    def __str__(self):
        return self.name
