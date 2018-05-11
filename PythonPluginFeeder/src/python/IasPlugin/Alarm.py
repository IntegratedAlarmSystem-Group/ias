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
    def fromString(alarmString):
        '''
        @param alarmString: the string representation of an Alarm like
                      Alarm.SET or SET
        @return the alarmString represented by the passed a string
        '''
        if alarmString is None or alarmString=="":
            raise ValueError("Invalid string representation of an alarmString")
        
        temp = str(alarmString)
        if "." not in temp:
            temp="Alarm."+temp
        for alarmState in Alarm:
            if str(alarmState)==temp:
                return alarmState
        # No enumerated matches with alarmString
        raise NotImplementedError("Not supported/find alarmString: "+alarmString)