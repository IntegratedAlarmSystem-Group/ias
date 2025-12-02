#! /usr/bin/env python3
'''
Created on Jun 8, 2018

@author: acaproni
'''
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp


class TestIsoTimestamp():
    
    tStamp = "2018-03-07T13:08:43.525"

    def test_tstamp(self):
        d = Iso8601TStamp.Iso8601ToDatetime(self.tStamp)
        s = Iso8601TStamp.datetimeToIsoTimestamp(d)
        assert self.tStamp == s
        
