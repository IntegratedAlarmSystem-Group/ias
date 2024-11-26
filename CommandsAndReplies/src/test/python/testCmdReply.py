#! /usr/bin/env python3
'''
Created on Dec 16, 2019

@author: acaproni
'''

import unittest

from time import sleep

from IASLogging.logConf import Log
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus
from IasCmdReply.IasCommand import IasCommand
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasReply import IasReply


class TestCmdReply(unittest.TestCase):
    '''
    Test IasCommand and IasReply
    '''

    def testCmdConstructor(self):
        now = Iso8601TStamp.now()
        cmd = IasCommand("DEST","SENDER", "PING", str(3), now, ["p1","p2"], {"a":"b", "A":"B"} )

        self.assertEqual(cmd.senderFullRunningId,"SENDER")
        self.assertEqual(cmd.destId,"DEST")
        self.assertEqual(cmd.command,IasCommandType.PING)
        self.assertEqual(cmd.id,"3")
        self.assertEqual(cmd.timestamp,now)
        self.assertEqual(cmd.params, ["p1","p2"])
        self.assertEqual(cmd.props,{"a":"b", "A":"B"})

        cmd2 = IasCommand("DEST2","SENDER2", IasCommandType.SHUTDOWN, str(1), "2019-12-15T09:55:21.002")
        self.assertEqual(cmd2.senderFullRunningId,"SENDER2")
        self.assertEqual(cmd2.destId,"DEST2")
        self.assertEqual(cmd2.command,IasCommandType.SHUTDOWN)
        self.assertEqual(cmd2.id,"1")
        self.assertEqual(cmd2.timestamp,"2019-12-15T09:55:21.002")
        self.assertIsNone(cmd2.params)
        self.assertIsNone(cmd2.props)

    def testCmdJsonSerialization(self):
        cmd = IasCommand("DEST","SENDER", "PING", str(3), "2019-12-16T09:47:21.172", ["p1","p2"], {"a":"b", "A":"B"} )
        cmdStr = cmd.toJSonString()
        cmdFromJson = IasCommand.fromJSon(cmdStr)
        self.assertEqual(cmdFromJson.senderFullRunningId,"SENDER")
        self.assertEqual(cmdFromJson.destId,"DEST")
        self.assertEqual(cmdFromJson.command,IasCommandType.PING)
        self.assertEqual(cmdFromJson.id,"3")
        self.assertEqual(cmdFromJson.timestamp,"2019-12-16T09:47:21.172")
        self.assertEqual(cmdFromJson.params, ["p1","p2"])
        self.assertEqual(cmdFromJson.props,{"a":"b", "A":"B"})

        cmd2 = IasCommand("DEST","SENDER", "PING", str(3), "2019-12-16T09:47:21.172")
        cmdStr = cmd2.toJSonString()
        cmdFromJson = IasCommand.fromJSon(cmdStr)
        self.assertEqual(cmdFromJson.senderFullRunningId,"SENDER")
        self.assertEqual(cmdFromJson.destId,"DEST")
        self.assertEqual(cmdFromJson.command,IasCommandType.PING)
        self.assertEqual(cmdFromJson.id,"3")
        self.assertEqual(cmdFromJson.timestamp,"2019-12-16T09:47:21.172")
        self.assertIsNone(cmdFromJson.params)
        self.assertIsNone(cmdFromJson.props)


    def testReplyConstructor(self):
        recTime = Iso8601TStamp.now()
        sleep(0.2)
        procTime = Iso8601TStamp.now()
        props = {"a":"b", "A":"B"}
        r = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime, props)
        self.assertEqual(r.senderFullRunningId,"TheSender")
        self.assertEqual(r.destFullRunningId,"TheDest")
        self.assertEqual(r.command,IasCommandType.ACK)
        self.assertEqual(r.id,"5")
        self.assertEqual(r.exitStatus,IasCmdExitStatus.REJECTED)
        self.assertEqual(r.receptionTStamp,recTime)
        self.assertEqual(r.processedTStamp,procTime)
        self.assertEqual(r.properties,props)

        r2 = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime)
        self.assertEqual(r2.senderFullRunningId,"TheSender")
        self.assertEqual(r2.destFullRunningId,"TheDest")
        self.assertEqual(r2.command,IasCommandType.ACK)
        self.assertEqual(r2.id,"5")
        self.assertEqual(r2.exitStatus,IasCmdExitStatus.REJECTED)
        self.assertEqual(r.receptionTStamp,recTime)
        self.assertEqual(r2.processedTStamp,procTime)
        self.assertIsNone(r2.properties)

    def testReplySerialization(self):
        recTime = Iso8601TStamp.now()
        sleep(0.2)
        procTime = Iso8601TStamp.now()
        props = {"a":"b", "A":"B"}
        r = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime, props)

        replyStr = r.toJSonString()
        replyFromJson = IasReply.fromJSon(replyStr)
        self.assertEqual(replyFromJson.senderFullRunningId,"TheSender")
        self.assertEqual(replyFromJson.destFullRunningId,"TheDest")
        self.assertEqual(replyFromJson.command,IasCommandType.ACK)
        self.assertEqual(replyFromJson.id,"5")
        self.assertEqual(replyFromJson.exitStatus,IasCmdExitStatus.REJECTED)
        self.assertEqual(replyFromJson.receptionTStamp,recTime)
        self.assertEqual(replyFromJson.processedTStamp,procTime)
        self.assertEqual(replyFromJson.properties,props)

        r2 = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime)

        replyStr = r2.toJSonString()
        replyFromJson = IasReply.fromJSon(replyStr)
        self.assertEqual(replyFromJson.senderFullRunningId,"TheSender")
        self.assertEqual(replyFromJson.destFullRunningId,"TheDest")
        self.assertEqual(replyFromJson.command,IasCommandType.ACK)
        self.assertEqual(replyFromJson.id,"5")
        self.assertEqual(replyFromJson.exitStatus,IasCmdExitStatus.REJECTED)
        self.assertEqual(replyFromJson.receptionTStamp,recTime)
        self.assertEqual(replyFromJson.processedTStamp,procTime)
        self.assertIsNone(replyFromJson.properties)


if __name__ == "__main__":
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()
