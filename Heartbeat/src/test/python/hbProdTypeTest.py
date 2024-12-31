#! /usr/bin/env python3
"""
Test the HB Status
"""
import unittest
from IasHeartbeat.IasHeartbeatProducerType import IasHeartbeatProducerType

class TestHbStatus(unittest.TestCase):
    def testFromString(self):
        self.assertEqual(IasHeartbeatProducerType.fromString("CLIENT"), IasHeartbeatProducerType.CLIENT)
        with self.assertRaises(NotImplementedError) as nie:
            IasHeartbeatProducerType.fromString("UnknownHbStatus")

if __name__ == "__main__":
    unittest.main()