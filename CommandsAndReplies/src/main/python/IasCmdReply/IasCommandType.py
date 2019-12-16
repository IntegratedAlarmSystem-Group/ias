'''
Created on Dec 16, 2019

@author: acaproni
'''

from enum import Enum

class IasCommandType(Enum):
    '''
    The supported command as defined in the java
    org.eso.ias.command.CommandType
    '''
    PING = 0 # Do nothing
    SHUTDOWN = 1 # Shuts down the process
    RESTART = 2 # Restart the process
    SET_LOG_LEVEL = 3 # Set the log level of the process
    ACK = 4 # ACK an alarm
    TF_CHANGED = 5 # A TF has been changed

    @staticmethod
    def fromString(cmdTypeStr):
        '''
        @param cmdTypeStr: the string representation of a CommandType like
                      IasCommandType.PING or PING
        @return the command represented by the passed string
        '''
        if not str:
            raise ValueError("Invalid string representation of a comamnd")

        temp = str(cmdTypeStr)
        if "." not in temp:
            temp="IasCommandType."+temp
        for cmdType in IasCommandType:
            if str(cmdType)==temp:
                return cmdType
        # No enumerated matches with cmdTypeStr
        raise NotImplementedError("Not supported/find IAS command type: " + cmdTypeStr)