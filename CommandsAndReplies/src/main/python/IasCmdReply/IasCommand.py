'''
Created on Dec 16, 2019

@author: acaproni
'''

import json

from IasCmdReply.IasCommandType import IasCommandType


class IasCommand(object):
    '''
    The python data structure for a Command to be sent to the kafka command topic.

    It is the python equivalent of the java org.eso.ias.command.CommandMessage

    All parameters are strings, the timestamps are string formatted as ISO-8601
    '''

    # The address to send a message to all the IAS tools */
    BROADCAST_ADDRESS = "*"

    # The full running ID of the sender of the command
    senderFullRunningId = None

    # The identifier of the receiver  (destination) of the command
    # it can be BROADCAST if the command is aimed to ALL the processes
    destId = None

    # The command to execute
    command = None

    # The unique identifier (in the context of the sender) of the command
    id = None

    # The parameters of the command, if any
    params = None

    # The timestamp when the command has been published (in ISO 8601 format)
    timestamp = None

    # Additional properties, if any
    props = None

    def __init__(self, dest, sender, cmd, id, tStamp, params=None, props=None):
        '''
        Constructor

        Build a command with the given parameters

        @param dest The ID of destination (or BROADCAST)
        @param sender The full running id of the sender (string)
        @param cmd The command to execute (a string or a IasCommandType)
        @param id The unique identifier of the command (int or string encoding an int)
        @param tStamp The timestamp when the command has been published (in ISO 8601 format)
        @param params The parameters of the command, if any (Nore or list of strings)
        @param props Additional properties, if any (None or dictionary of strings)
        '''
        if not dest:
            raise ValueError("Invalid destination of command")
        self.destId = dest

        if not sender:
            raise ValueError("Invalid full running ID of the sender of the command")
        self.senderFullRunningId = sender

        if not cmd:
            raise ValueError("Invalid command")
        if isinstance(cmd,IasCommandType):
            self.command = cmd.name
        else:
            self.command = cmd

        if not id:
            raise ValueError("Invalid id of the command")
        self.id = str(id)

        if not tStamp:
            raise ValueError("Invalid timestamp")
        self.timestamp = tStamp

        self.params = params
        self.props = props

    def toJSonString(self):
        """
        @return the JSON representation of the IasCommand
        """
        temp = {}
        temp["destId"] = self.destId
        temp["command"] = self.command
        temp["senderFullRunningId"] = self.senderFullRunningId
        temp["id"] = self.id
        temp["timestamp"] = self.timestamp
        if self.params is not None and len(self.params)>0:
            temp ["params"] = self.params
        if self.props is not None and len(self.props)>0:
            temp["properties"] = self.props

        return json.dumps(temp)

    @staticmethod
    def fromJSon(jsonStr):
        '''
        Factory method to build a IasCommand from a JSON string

        @param jsonStr the json string representing the IasCommand
        '''
        if not jsonStr:
            raise ValueError("Invalid JSON string to parse")

        fromJsonDict = json.loads(jsonStr)

        return IasCommand(
            fromJsonDict["destId"],
            fromJsonDict["senderFullRunningId"],
            fromJsonDict["command"],
            fromJsonDict["id"],
            fromJsonDict["timestamp"],
            fromJsonDict["params"],
            fromJsonDict["properties"]
        )
