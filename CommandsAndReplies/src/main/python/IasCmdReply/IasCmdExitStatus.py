'''
Created on Dec 16, 2019

@author: acaproni
'''

from enum import Enum

class IasCmdExitStatus(Enum):
    '''
    Th exit status of the ocmmand.

    It is the python implementation of the java org.eso.ias.command.CommandExitStatus
    '''
    OK = 0,
    REJECTED = 1
    ERROR = 2
    UNKNOWN = 3

    @staticmethod
    def fromString(exitStatusStr):
        '''
        @param exitStatusStr: the string representation of exit status of a command like
                      IasCmdExitStatus.OK or OK
        @return the command exit status represented by the passed string
        '''
        if not str:
            raise ValueError("Invalid string representation of the exit status of a comamnd")

        temp = str(exitStatusStr)
        if "." not in temp:
            temp="exitStatusStr."+temp
        for exitStatus in IasCmdExitStatus:
            if str(exitStatus)==temp:
                return exitStatus
        # No enumerated matches with exitStatus
        raise NotImplementedError("Not supported/find IAS command exit status: " + exitStatusStr)