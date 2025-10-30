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

    def __init__(self, dest: str, sender: str, cmd, id: int, tStamp: str, params=None, props=None):
        '''
        Constructor

        Build a command with the given parameters

        Params:
            dest The ID of destination (or BROADCAST)
            sender The full running id of the sender (string)
            cmd The command to execute (a string or a IasCommandType)
            id The unique identifier of the command (int or string encoding an int)
            tStamp The timestamp when the command has been published (in ISO 8601 format)
            params The parameters of the command, if any (None or list of strings)
            props Additional properties, if any (None or dictionary of strings)
        '''
        if not dest:
            raise ValueError("Invalid destination of command")
        # The identifier of the receiver  (destination) of the command
        # it can be BROADCAST if the command is aimed to ALL the processes
        self.destId = dest

        if not sender:
            raise ValueError("Invalid full running ID of the sender of the command")
        # The full running ID of the sender of the command
        self.senderFullRunningId = sender

        if not cmd:
            raise ValueError("Invalid command")
        # The command to execute
        if isinstance(cmd,IasCommandType):
            self.command = cmd
        else:
            self.command = IasCommandType.fromString(cmd)

        if not id:
            raise ValueError("Invalid id of the command")
        # The unique identifier (in the context of the sender) of the command
        self.id = str(id)

        if not tStamp:
            raise ValueError("Invalid timestamp")
        # The timestamp when the command has been published (in ISO 8601 format)
        self.timestamp = tStamp

        # The parameters of the command, if any
        self.params = params

        # Additional properties, if any
        self.props = props

    def toJSonString(self):
        """
        @return the JSON representation of the IasCommand
        """
        temp = {}
        temp["destId"] = self.destId
        temp["command"] = self.command.name
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

        if "params" in fromJsonDict:
            params = fromJsonDict["params"]
        else:
            params = None

        if "properties" in fromJsonDict:
            props = fromJsonDict["properties"]
        else:
            props = None

        return IasCommand(
            fromJsonDict["destId"],
            fromJsonDict["senderFullRunningId"],
            IasCommandType.fromString(fromJsonDict["command"]),
            int(fromJsonDict["id"]),
            fromJsonDict["timestamp"],
            params,
            props
        )
