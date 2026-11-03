'''
Created on May 10, 2018

@author: acaproni
'''
from enum import Enum

class OperationalMode(Enum):
    '''
    The operational mode of a monitor point or alarm 
    as defined in org.eso.ias.types.OperationalMode
    '''
    # Shutting down
    CLOSING = 0

    # Only partially operational
    DEGRADED = 1

    # Initialization on going
    INITIALIZATION = 2

    # Maintenance
    MAINTENANCE = 3

    # Fully operational
    OPERATIONAL = 4

    # Shutted down
    SHUTTEDDOWN = 5

    # Starting up
    STARTUP = 6

    # Unknown state
    UNKNOWN = 7

    # Malfunctioning
    MALFUNCTIONING = 8
    
    @staticmethod
    def fromString(alarmString):
        '''
        @param alarmString: the string representation of OperationalMode like
                      OperationalMode.DEGRADED or DEGRADED
        @return the mode represented by the passed a string
        '''
        if not str:
            raise ValueError("Invalid string representation of a operational mode")
        
        temp = str(alarmString)
        if "." not in temp:
            temp="OperationalMode."+temp
        for mode in OperationalMode:
            if str(mode)==temp:
                return mode
        # No enumerated matches with alarmString
        raise NotImplementedError("Not supported/find operational mode: "+alarmString)
        