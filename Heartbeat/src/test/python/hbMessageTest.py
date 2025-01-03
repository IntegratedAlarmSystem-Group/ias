#! /usr/bin/env python3
import unittest

from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus

class HbMessageTest(unittest.TestCase):
    """
    Test the HeartbeatMessage serialization/deserialization
    """
    
    def testFromStrNoProps(self):
        hbMsgStr = '{"timestamp":"2025-01-03T09:58:56.641","hbStringrepresentation":"SupervisorWithKafka:SUPERVISOR@ias-fc40","state":"RUNNING"}'
        hbMsg = HeartbeatMessage.fromJSON(hbMsgStr)

        self.assertEqual(hbMsg.timestamp, "2025-01-03T09:58:56.641")
        self.assertEqual(hbMsg.hbStringrepresentation, "SupervisorWithKafka:SUPERVISOR@ias-fc40")
        self.assertEqual(hbMsg.state, "RUNNING")
        self.assertIsNone(hbMsg.props)

    def testFromStrWithProps(self):
        hbMsgStr = '{"timestamp":"2025-01-03T11:05:57.361","hbStringrepresentation":"SupervisorID:SUPERVISOR@ias-fc40","state":"RUNNING","props":{"key1":"prop1","key2":"prop2"}}'
        hbMsg = HeartbeatMessage.fromJSON(hbMsgStr)

        self.assertEqual(hbMsg.timestamp, "2025-01-03T11:05:57.361")
        self.assertEqual(hbMsg.hbStringrepresentation, "SupervisorID:SUPERVISOR@ias-fc40")
        self.assertEqual(hbMsg.state, "RUNNING")
        self.assertEqual(hbMsg.props, {"key1":"prop1","key2":"prop2"})

    def fromHbNoProps(self):
        hbMsgStr = '{"timestamp":"2025-01-03T09:58:56.641","hbStringrepresentation":"SupervisorWithKafka:SUPERVISOR@ias-fc40","state":"PARTIALLY_RUNNING"}'
        hbMsg = HeartbeatMessage("SupervisorWithKafka:SUPERVISOR@ias-fc40",
                                 IasHeartbeatStatus.PARTIALLY_RUNNING,
                                 None,
                                 "2025-01-03T09:58:56.641")
        json_str = hbMsg.toJSON()
        self.assertEqual(json_str, hbMsgStr)

    def fromHbWithProps(self):
        hbMsgStr = '{"timestamp":"2025-01-03T11:05:57.361","hbStringrepresentation":"SupervisorID:SUPERVISOR@ias-fc40","state":"RUNNING","props":{"key1":"prop1","key2":"prop2"}}'
        hbMsg = HeartbeatMessage("SupervisorID:SUPERVISOR@ias-fc40",
                                 IasHeartbeatStatus.RUNNING,
                                 {"key1":"prop1","key2":"prop2"},
                                 "2025-01-03T11:05:57.361")
        json_str = hbMsg.toJSON()
        self.assertEqual(json_str, hbMsgStr)

if __name__ == "__main__":
    unittest.main()