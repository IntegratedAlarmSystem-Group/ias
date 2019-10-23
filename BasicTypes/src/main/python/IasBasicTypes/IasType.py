'''
Created on May 10, 2018

@author: acaproni
'''
from enum import Enum

from IasBasicTypes.Alarm import Alarm


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
    TIMESTAMP = 10
    ARRAYOFDOUBLES = 11
    ARRAYOFLONGS = 12
    ALARM = 13

    def parseStringOfArray(self,value):
        '''
        Parse the passed string of array and return a list of integers or doubles

        The string is formatted like [0.123,-99.05,2,3,5,7]

        :param self:
        :param value: The value to convert: it is string representing an array
        :return: the list with integer or doubles in the string
        '''
        print("Parsing",value,"of type",self)
        ret = []
        value = value.replace('[',"").replace(']',"").strip()
        values = value.split(',')
        for num in values:
            if self==IASType.ARRAYOFLONGS:
                ret.append(int(num))
            elif self==IASType.ARRAYOFDOUBLES:
                ret.append(float(num))
            else:
                raise NotImplementedError("Not supported conversion of array for IAS data type: "+self)
        print("Returning",ret)
        return ret
    
    def convertStrToValue(self,value):
        '''
        Convert the passed string into a value of the proper type
        '''
        if value is None or value=="":
            raise ValueError("Invalid none or empty string to convert")
        
        if self==IASType.LONG or \
            self==IASType.INT or \
            self==IASType.SHORT or \
            self==IASType.BYTE:
            return int(value)
        elif self==IASType.DOUBLE or \
            self==IASType.FLOAT:
            return float(value)
        elif self==IASType.BOOLEAN:
            if value.upper() in ["TRUE", "1"]:
                return True
            elif value.upper() in ["FALSE", "0"]:
                return False
            else:
                raise ValueError("Invalid boolean string: "+value)
        elif self==IASType.CHAR:
            return value[0]
        elif self==IASType.STRING:
            return value
        elif self==IASType.ALARM:
            return Alarm.fromString(value)
        elif self==IASType.TIMESTAMP:
            pass
        elif self==IASType.ARRAYOFLONGS or \
            self==IASType.ARRAYOFDOUBLES:
            return self.parseStringOfArray(value)
        else:
            raise NotImplementedError("Not supported conversion for IAS data type: "+self)



    @staticmethod
    def fromString(alarmString):
        '''
        @param alarmString: the string representation of IasType like
                      IASType.DOUBLE or DOUBLE 
        @return the type represented by the passed a string
        '''
        if not str:
            raise ValueError("Invalid string representation of a type")
        
        temp = str(alarmString)
        if "." not in temp:
            temp="IASType."+temp
        for valueType in IASType:
            if str(valueType)==temp:
                return valueType
        # No enumerated matches with alarmString
        raise NotImplementedError("Not supported/find IAS data type"+alarmString)
    