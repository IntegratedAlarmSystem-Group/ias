#! /usr/bin/env python
'''
Created on Jun 8, 2018

@author: acaproni
'''
import unittest
from IasSupport.Iso8601TStamp import Iso8601TStamp


class IsoTimestampTest(unittest.TestCase):
    
    tStamp = "2018-03-07T13:08:43.525"

    def testTStamp(self):
        d = Iso8601TStamp.Iso8601ToDatetime(self.tStamp)
        s = Iso8601TStamp.datetimeToIsoTimestamp(d)
        self.assertEqual(self.tStamp, s)
        


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()