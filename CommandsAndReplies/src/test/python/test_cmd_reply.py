'''
Created on Dec 16, 2019

@author: acaproni
'''
from time import sleep
from IASLogging.logConf import Log
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus
from IasCmdReply.IasCommand import IasCommand
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasReply import IasReply


class TestCmdReply():
    '''
    Test IasCommand and IasReply
    '''

    def test_cmd_constructor(self):
        now = Iso8601TStamp.now()
        cmd = IasCommand("DEST","SENDER", "PING", str(3), now, ["p1","p2"], {"a":"b", "A":"B"} )

        assert cmd.senderFullRunningId == "SENDER"
        assert cmd.destId == "DEST"
        assert cmd.command == IasCommandType.PING
        assert cmd.id == "3"
        assert cmd.timestamp == now
        assert cmd.params == ["p1","p2"]
        assert cmd.props == {"a":"b", "A":"B"}

        cmd2 = IasCommand("DEST2","SENDER2", IasCommandType.SHUTDOWN, str(1), "2019-12-15T09:55:21.002")
        assert cmd2.senderFullRunningId == "SENDER2"
        assert cmd2.destId == "DEST2"
        assert cmd2.command == IasCommandType.SHUTDOWN
        assert cmd2.id == "1"
        assert cmd2.timestamp == "2019-12-15T09:55:21.002"
        assert cmd2.params is None
        assert cmd2.props is None

    def test_cmd_json_serialization(self):
        cmd = IasCommand("DEST","SENDER", "PING", str(3), "2019-12-16T09:47:21.172", ["p1","p2"], {"a":"b", "A":"B"} )
        cmdStr = cmd.toJSonString()
        cmdFromJson = IasCommand.fromJSon(cmdStr)
        assert cmdFromJson.senderFullRunningId == "SENDER"
        assert cmdFromJson.destId == "DEST"
        assert cmdFromJson.command == IasCommandType.PING
        assert cmdFromJson.id == "3"
        assert cmdFromJson.timestamp == "2019-12-16T09:47:21.172"
        assert cmdFromJson.params == ["p1","p2"]
        assert cmdFromJson.props == {"a":"b", "A":"B"}

        cmd2 = IasCommand("DEST","SENDER", "PING", str(3), "2019-12-16T09:47:21.172")
        cmdStr = cmd2.toJSonString()
        cmdFromJson = IasCommand.fromJSon(cmdStr)
        assert cmdFromJson.senderFullRunningId == "SENDER"
        assert cmdFromJson.destId == "DEST"
        assert cmdFromJson.command == IasCommandType.PING
        assert cmdFromJson.id == "3"
        assert cmdFromJson.timestamp == "2019-12-16T09:47:21.172"
        assert cmdFromJson.params is None
        assert cmdFromJson.props is None


    def test_reply_constructor(self):
        recTime = Iso8601TStamp.now()
        sleep(0.2)
        procTime = Iso8601TStamp.now()
        props = {"a":"b", "A":"B"}
        r = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime, props)
        assert r.senderFullRunningId == "TheSender"
        assert r.destFullRunningId == "TheDest"
        assert r.command == IasCommandType.ACK
        assert r.id == "5"
        assert r.exitStatus == IasCmdExitStatus.REJECTED
        assert r.receptionTStamp == recTime
        assert r.processedTStamp == procTime
        assert r.properties == props

        r2 = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime)
        assert r2.senderFullRunningId == "TheSender"
        assert r2.destFullRunningId == "TheDest"
        assert r2.command == IasCommandType.ACK
        assert r2.id == "5"
        assert r2.exitStatus == IasCmdExitStatus.REJECTED
        assert r.receptionTStamp == recTime
        assert r2.processedTStamp == procTime
        assert r2.properties is None

    def test_reply_serialization(self):
        recTime = Iso8601TStamp.now()
        sleep(0.2)
        procTime = Iso8601TStamp.now()
        props = {"a":"b", "A":"B"}
        r = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime, props)

        replyStr = r.toJSonString()
        replyFromJson = IasReply.fromJSon(replyStr)
        assert replyFromJson.senderFullRunningId == "TheSender"
        assert replyFromJson.destFullRunningId == "TheDest"
        assert replyFromJson.command == IasCommandType.ACK
        assert replyFromJson.id == "5"
        assert replyFromJson.exitStatus == IasCmdExitStatus.REJECTED
        assert replyFromJson.receptionTStamp == recTime
        assert replyFromJson.processedTStamp == procTime
        assert replyFromJson.properties == props

        r2 = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime)

        replyStr = r2.toJSonString()
        replyFromJson = IasReply.fromJSon(replyStr)
        assert replyFromJson.senderFullRunningId == "TheSender"
        assert replyFromJson.destFullRunningId == "TheDest"
        assert replyFromJson.command == IasCommandType.ACK
        assert replyFromJson.id == "5"
        assert replyFromJson.exitStatus == IasCmdExitStatus.REJECTED
        assert replyFromJson.receptionTStamp == recTime
        assert replyFromJson.processedTStamp == procTime
        assert replyFromJson.properties is None
