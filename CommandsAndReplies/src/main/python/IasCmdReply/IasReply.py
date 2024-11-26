'''
Created on Dec 16, 2019

@author: acaproni
'''

import json

from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus
from IasCmdReply.IasCommandType import IasCommandType


class IasReply(object):
    '''
    The python data structure for a Reply to be published in the kafka reply topic.

    It is the python equivalent of the java org.eso.ias.command.ReplyMessage

    All parameters are strings, the timestamps are string formatted as ISO-8601
    '''

    # The full running ID of the sender of the reply
    senderFullRunningId = None

    # The full running ID of the receiver of the command.
    destFullRunningId = None

    # The unique identifier (in the context of the sender) of the command.
    id: str = None

    # The command just executed
    command: IasCommandType = None

    #  The exit status of the command
    exitStatus: IasCmdExitStatus = None

    # The point in time when the command has been received from the kafka topic (ISO 8601)
    receptionTStamp = None

    # The point in time when the execution of the command terminated (ISO 8601)
    processedTStamp = None

    # Additional properties, if any
    properties = None

    def __init__(self, sender, dest, cmd: IasCommandType|str, id, exitStatus: IasCmdExitStatus|str, recvTStamp, procTStamp, properties=None):
        if not sender:
            raise ValueError("Invalid sender")
        self.senderFullRunningId = sender
        if not dest:
            raise ValueError("Invalid destination")
        self.destFullRunningId = dest
        if not cmd:
            raise ValueError("Invalid command")
        if isinstance(cmd,IasCommandType):
            self.command = cmd
        else:
            self.command = IasCommandType.fromString(cmd)

        if not id:
            raise ValueError("Invalid id of the command")
        self.id = str(id)

        if not exitStatus:
            raise ValueError("Invalid result of command")
        if isinstance(exitStatus,IasCmdExitStatus):
            self.exitStatus = exitStatus
        else:
            self.exitStatus = IasCmdExitStatus.fromString(exitStatus)

        if not recvTStamp:
            raise ValueError("Invalid time of reception")
        self.receptionTStamp = recvTStamp
        if not procTStamp:
            raise ValueError("Invalid processing timestamp")
        self.processedTStamp = procTStamp

        self.properties = properties

    def toJSonString(self):
        """
        @return the JSON representation of the IasReply
        """
        temp = {}
        temp["destFullRunningId"] = self.destFullRunningId
        temp["command"] = self.command.name
        temp["senderFullRunningId"] = self.senderFullRunningId
        temp["id"] = self.id
        temp["exitStatus"] = self.exitStatus.name
        temp["receptionTStamp"] = self.receptionTStamp
        temp["processedTStamp"] = self.processedTStamp
        if self.properties is not None and len(self.properties)>0:
            temp["properties"] = self.properties

        return json.dumps(temp)

    @staticmethod
    def fromJSon(jsonStr):
        '''
        Factory method to build a IasReply from a JSON string

        @param jsonStr the json string representing the IasCommand
        '''
        if not jsonStr:
            raise ValueError("Invalid JSON string to parse")

        fromJsonDict = json.loads(jsonStr)

        if "properties" in fromJsonDict:
            props = fromJsonDict["properties"]
        else:
            props = None

        return IasReply(
            fromJsonDict["senderFullRunningId"],
            fromJsonDict["destFullRunningId"],
            IasCommandType.fromString(fromJsonDict["command"]),
            fromJsonDict["id"],
            IasCmdExitStatus.fromString(fromJsonDict["exitStatus"]),
            fromJsonDict["receptionTStamp"],
            fromJsonDict["processedTStamp"],
            props
        )