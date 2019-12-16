'''
Created on Dec 16, 2019

@author: acaproni
'''

import json

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
    id = None

    # The command just executed
    command = None

    #  The exit status of the command
    exitStatus = None

    # The point in time when the command has been received from the kafka topic (ISO 8601)
    receptionTStamp = None

    # The point in time when the execution of the command terminated (ISO 8601)
    processedTStamp = None

    # Additional properties, if any
    properties = None

    def __init__(self, sender, dest, cmd, id, exitStatus, recvTStamp, procTStamp, properties=None):
        if not sender:
            raise ValueError("Invalid sender")
        self.senderFullRunningId = sender
        if not dest:
            raise ValueError("Invalid destination")
        self.destFullRunningId = dest
        if not cmd:
            raise ValueError("Invalid command")
        self.command = cmd
        if not id:
            raise ValueError("Invalid id of the command")
        self.id = str(id)
        if not exitStatus:
            raise ValueError("Invalid result of command")
        self.exitStatus = exitStatus
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
        temp["command"] = self.command
        temp["senderFullRunningId"] = self.senderFullRunningId
        temp["id"] = self.id
        temp["exitStatus"] = self.exitStatus
        temp["receptionTStamp"] = self.receptionTStamp
        temp["processedTStamp"] = self.processedTStamp
        if self.props is not None and len(self.props)>0:
            temp["properties"] = self.props

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

        return IasReply(
            fromJsonDict["senderFullRunningId"],
            fromJsonDict["destFullRunningId"],
            fromJsonDict["command"],
            fromJsonDict["id"],
            fromJsonDict["exitStatus"],
            fromJsonDict["receptionTStamp"],
            fromJsonDict["processedTStamp"],
            fromJsonDict["properties"]
        )