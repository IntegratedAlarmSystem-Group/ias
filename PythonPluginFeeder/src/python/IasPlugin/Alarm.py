'''
Created on May 10, 2018

@author: acaproni
'''
from enum import Enum

class Alarm(Enum):
    '''
    The alarm as defined in org.eso.ias.types.Alarm
    '''
    # Critical alarm set
    SET_CRITICAL = 4
    
    #  HIGH alarm set
    SET_HIGH = 3
    
    # Medium alarm set
    SET_MEDIUM = 2
    
    #  Low priority alarm set
    SET_LOW = 1
    
    #  Alarm clear or unset
    CLEARED = 0
    
    @staticmethod
    def fromString(alarmString):
        '''
        @param alarmString: the string representation of an Alarm like
                      Alarm.SET_CRITICAL or SET_MEDIUM
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
    
    @staticmethod
    def getSetDefault():
        '''
        @return the default priority for an alarm set
        '''
        return Alarm.SET_MEDIUM
    
    @staticmethod
    def cleared(self):
        '''
        A method returning the cleared alarm
        
        @return the cleared alarm
        '''
        return Alarm.CLEARED
    
    @staticmethod
    def getMaxPriorityAlarm():
        '''
        @return the alarm with the highest priority
        '''
        return Alarm.SET_CRITICAL
    
    @staticmethod
    def getMinPriorityAlarm():
        '''
        @return the alarm with the lowest priority
        '''
        return Alarm.SET_LOW
    
    def increasePriority(self):
        '''
        Return an alarm of a lowered priority if it exists,
        otherwise return the same alarm.
      
        Lowering the priority of a {@link #CLEARED} 
        alarm is not allowed and the method throws an exception
      
        @return the alarm with increased priority
        '''
        if (self.value==0):
            raise ValueError("Cannot increase the priority of an alarm that is not set")
        elif self.value==1:
            return Alarm.SET_MEDIUM
        if (self.value==2):
            return Alarm.SET_HIGH
        if (self.value==3 or self.value==4):
            Alarm.SET_CRITICAL
    
    def lowerPriority(self):
        '''
        Return an alarm of a lowered priority if it exists,
        otherwise return the same alarm.
         
        Lowering the priority of a CLEARED 
        alarm is not allowed and the method throws an exception
         
        @return the alarm with increased priority
        '''
        if (self.value==0):
            raise ValueError("Cannot lower the priority of an alarm that is not set")
        elif self.value==1 or self.value==2:
            return Alarm.SET_LOW
        if (self.value==3):
            return Alarm.SET_MEDIUM
        if (self.value==4):
            Alarm.SET_HIGH
    
    def isSet(self):
        '''
        @return True if the alarm is set and False otherwise
        '''
        return self.value>0       
    