#! /usr/bin/env python3
'''

test for JsonMsg

Created on May 10, 2018

@author: acaproni
'''
import datetime
import unittest

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode
from IasPlugin3.JsonMsg import JsonMsg


class TestJsonMessage(unittest.TestCase):
    
    def setUp(self):
        pass
    
    def tearDown(self):
        pass
    
    def testTypeConversionsDouble(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        tStamp = datetime.datetime.utcnow()
        msg = JsonMsg("MPoint-ID", 2.3, IASType.DOUBLE,tStamp,OperationalMode.CLOSING)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-ID")
        self.assertEqual(fromJString.value, 2.3)
        self.assertEqual(fromJString.valueType,IASType.DOUBLE)
        self.assertEqual(fromJString.operationalMode, OperationalMode.CLOSING) 
        
    def testTypeConversionsLong(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        tStamp = datetime.datetime.utcnow()
        msg = JsonMsg("MPoint-IDL", 97, IASType.LONG,tStamp,OperationalMode.DEGRADED)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-IDL")
        self.assertEqual(fromJString.value, 97)
        self.assertEqual(fromJString.valueType,IASType.LONG)
        self.assertEqual(fromJString.operationalMode, OperationalMode.DEGRADED)
        
    def testTypeConversionsString(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDS", "A Test String", IASType.STRING,operationalMode=OperationalMode.OPERATIONAL)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-IDS")
        self.assertEqual(fromJString.value, "A Test String")
        self.assertEqual(fromJString.valueType,IASType.STRING)
        self.assertEqual(fromJString.operationalMode, OperationalMode.OPERATIONAL)
        
    def testTypeConversionsBool(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDB", True, IASType.BOOLEAN,operationalMode=OperationalMode.INITIALIZATION)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-IDB")
        self.assertEqual(fromJString.value, True)
        self.assertEqual(fromJString.valueType,IASType.BOOLEAN)
        self.assertEqual(fromJString.operationalMode, OperationalMode.INITIALIZATION)
        
    def testTypeConversionsChar(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDC", 'X', IASType.CHAR,operationalMode=OperationalMode.CLOSING)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-IDC")
        self.assertEqual(fromJString.value, 'X')
        self.assertEqual(fromJString.valueType,IASType.CHAR)
        self.assertEqual(fromJString.operationalMode, OperationalMode.CLOSING)
        
    def testTypeConversionsAlarm(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDC", Alarm.SET_MEDIUM, IASType.ALARM,operationalMode=OperationalMode.SHUTTEDDOWN)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-IDC")
        self.assertEqual(fromJString.value, Alarm.SET_MEDIUM)
        self.assertEqual(fromJString.valueType,IASType.ALARM)
        self.assertEqual(fromJString.operationalMode, OperationalMode.SHUTTEDDOWN)

    def testTypeConversionsArrayLongs(self):
        msg = JsonMsg("MPoint-ARRAY-LONGS", [1,2,3,4,5,-1], IASType.ARRAYOFLONGS,operationalMode=OperationalMode.OPERATIONAL)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-ARRAY-LONGS")
        self.assertEqual(fromJString.value, [1,2,3,4,5,-1])
        self.assertEqual(fromJString.valueType,IASType.ARRAYOFLONGS)
        self.assertEqual(fromJString.operationalMode, OperationalMode.OPERATIONAL)

    def testTypeConversionsArrayDoubles(self):
        msg = JsonMsg("MPoint-ARRAY-DOUBLES", [0,0.123,-987.321,-1], IASType.ARRAYOFDOUBLES,operationalMode=OperationalMode.OPERATIONAL)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        self.assertEqual(fromJString.mPointID, "MPoint-ARRAY-DOUBLES")
        self.assertEqual(fromJString.value, [0,0.123,-987.321,-1])
        self.assertEqual(fromJString.valueType,IASType.ARRAYOFDOUBLES)
        self.assertEqual(fromJString.operationalMode, OperationalMode.OPERATIONAL)

if __name__ == '__main__':
    unittest.main()
