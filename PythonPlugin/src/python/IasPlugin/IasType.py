'''
Created on May 10, 2018

@author: acaproni
'''
from enum import Enum
class IASType(Enum):
    '''
    The supported data types 
    as defined in org.eso.ias.types.IASTypes
    '''
    LONG = 1 
    INT = 2 
    SHORT = 3 
    BYTE = 4
    DOUBLE = 5 
    FLOAT = 6
    BOOLEAN = 7 
    CHAR = 8
    STRING = 9 
    ALARM = 10
    
    @staticmethod
    def fromString(alarm):
        '''
        @param alarm: the string representation of IasType like
                      IASType.DOUBLE or DOUBLE 
        @return the type represented by the passed a string
        '''
        if not str:
            raise ValueError("Invalid string representation of a type")
        
        temp = str(alarm)
        if "." not in temp:
            temp="IASType."+temp
        for valueType in IASType:
            if str(valueType)==temp:
                return valueType
        # No enumerated matches with alarm
        raise NotImplementedError("Not supported/find IAS data type")
    