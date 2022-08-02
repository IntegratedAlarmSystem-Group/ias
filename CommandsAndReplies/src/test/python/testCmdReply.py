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
        self.assertEqual(cmd.command,"PING")
        self.assertEqual(cmd.id,"3")
        self.assertEqual(cmd.timestamp,now)
        self.assertEqual(cmd.params, ["p1","p2"])
        self.assertEqual(cmd.props,{"a":"b", "A":"B"})

        cmd2 = IasCommand("DEST2","SENDER2", IasCommandType.SHUTDOWN, str(1), "2019-12-15T09:55:21.002")
        self.assertEqual(cmd2.senderFullRunningId,"SENDER2")
        self.assertEqual(cmd2.destId,"DEST2")
        self.assertEqual(cmd2.command,"SHUTDOWN")
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
        self.assertEqual(cmdFromJson.command,"PING")
        self.assertEqual(cmdFromJson.id,"3")
        self.assertEqual(cmdFromJson.timestamp,"2019-12-16T09:47:21.172")
        self.assertEqual(cmdFromJson.params, ["p1","p2"])
        self.assertEqual(cmdFromJson.props,{"a":"b", "A":"B"})

    def testReply(self):
        recTime = Iso8601TStamp.now()
        sleep(0.2)
        procTime = Iso8601TStamp.now()
        props = {"a":"b", "A":"B"}
        r = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime, props)
        self.assertTrue(r.senderFullRunningId,"TheSender")
        self.assertTrue(r.destFullRunningId,"TheDest")
        self.assertTrue(r.command,"ACK")
        self.assertTrue(r.id,"5")
        self.assertTrue(r.exitStatus,"REJECTED")
        self.assertTrue(r.receptionTStamp,recTime)
        self.assertTrue(r.processedTStamp,procTime)
        self.assertTrue(r.properties,props)

    def testReplySerialization(self):
        recTime = Iso8601TStamp.now()
        sleep(0.2)
        procTime = Iso8601TStamp.now()
        props = {"a":"b", "A":"B"}
        r = IasReply("TheSender", "TheDest", IasCommandType.ACK, "5", IasCmdExitStatus.REJECTED, recTime, procTime, props)

        replyStr = r.toJSonString()
        replyFromJson = IasReply.fromJSon(replyStr)
        self.assertTrue(replyFromJson.senderFullRunningId,"TheSender")
        self.assertTrue(replyFromJson.destFullRunningId,"TheDest")
        self.assertTrue(replyFromJson.command,"ACK")
        self.assertTrue(replyFromJson.id,"5")
        self.assertTrue(replyFromJson.exitStatus,"REJECTED")
        self.assertTrue(replyFromJson.receptionTStamp,recTime)
        self.assertTrue(replyFromJson.processedTStamp,procTime)
        self.assertTrue(replyFromJson.properties,props)


if __name__ == "__main__":
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()
