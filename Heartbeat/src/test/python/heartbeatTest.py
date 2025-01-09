#! /usr/bin/env python3
"""
Test the Heartbeat
"""
import unittest
from IasHeartbeat.IasHeartbeat import IasHeartbeat
from IasHeartbeat.IasHeartbeatProducerType import IasHeartbeatProducerType

class TestHearbeat(unittest.TestCase):
    def testHeartbeatConstr(self):
        stringRepr = "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        hbType = IasHeartbeatProducerType.SUPERVISOR
        name = "SupervisorWithKafka"
        hostname = "ias-fc40"
        hb = IasHeartbeat(hbType=hbType, name=name, hostName=hostname)

        self.assertEqual(hb.stringRepr, stringRepr)
        self.assertEqual(hb.id, "SupervisorWithKafka:SUPERVISOR")

    def testHeartbeatFromStr(self):
        stringRepr = "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        hb = IasHeartbeat.fromStringRepr(stringRepr)
        self.assertEqual(stringRepr, hb.stringRepr)
        self.assertEqual(hb.name,"SupervisorWithKafka")
        self.assertEqual(hb.hostname , "ias-fc40")
        self.assertEqual(hb.hbType, IasHeartbeatProducerType.SUPERVISOR)

if __name__ == "__main__":
    unittest.main()