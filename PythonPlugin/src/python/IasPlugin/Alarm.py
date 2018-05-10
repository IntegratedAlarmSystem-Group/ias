'''
Created on May 10, 2018

@author: acaproni
'''
from enum import Enum

class Alarm(Enum):
    '''
    The alarm as defined in org.eso.ias.types.AlarmSample
    '''
    # Alarm raised
    SET = 1
    # Alarm cleared or unset
    CLEARED = 2

    @staticmethod
    def fromString(opMode):
        '''
        @param opMode: the string representation of an Alarm like
                      Alarm.SET or SET
        @return the opMode represented by the passed a string
        '''
        if not str:
            raise ValueError("Invalid string representation of an opMode")
        
        temp = str(opMode)
        if "." not in temp:
            temp="Alarm."+temp
        for alarmState in Alarm:
            if str(alarmState)==temp:
                return alarmState
        # No enumerated matches with opMode
        raise NotImplementedError("Not supported/find opMode")