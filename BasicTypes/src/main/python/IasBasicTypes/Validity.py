'''
Created on Jun 8, 2018

@author: acaproni
'''
from enum import Enum

class Validity(Enum):
    '''
    The validity as defined in IasValidity.java
    '''

    RELIABLE = 1
    
    UNRELIABLE = 0 
    
    @staticmethod
    def fromString(validityString):
        '''
        @param validityString: the string representation of a validity
                                like RELIABLE
        @return the Validity represented by the passed a string
        '''
        if validityString is None or validityString=="":
            raise ValueError("Invalid string representation of validity")
        
        temp = str(validityString)
        if "." not in temp:
            temp="Validity."+temp
        for validityState in Validity:
            if str(validityState)==temp:
                return validityState
        # No enumerated matches with alarmString
        raise NotImplementedError("Not supported/find validity: "+validityString)
    
    def toString(self):
        """
        @return the string representation of this validity
        """
        return self.name()
    