#! /usr/bin/env python3
"""
Test the HB Status
"""
import unittest
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus

class TestHbStatus(unittest.TestCase):
    def testFromString(self):
        self.assertEqual(IasHeartbeatStatus.fromString("PARTIALLY_RUNNING"), IasHeartbeatStatus.PARTIALLY_RUNNING)
        with self.assertRaises(NotImplementedError) as nie:
            IasHeartbeatStatus.fromString("UnknownHbStatus")

if __name__ == "__main__":
    unittest.main()