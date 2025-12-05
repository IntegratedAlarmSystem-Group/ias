'''
test for JsonMsg

Created on May 10, 2018

@author: acaproni
'''
import datetime
from datetime import timezone

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.Priority import Priority
from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode
from IasPlugin3.JsonMsg import JsonMsg


class TestJsonMessage():
    
    def test_type_conversions_double(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        tStamp = datetime.datetime.now(timezone.utc)
        msg = JsonMsg("MPoint-ID", 2.3, IASType.DOUBLE,tStamp,OperationalMode.CLOSING)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-ID"
        assert fromJString.value == 2.3
        assert fromJString.valueType == IASType.DOUBLE
        assert fromJString.operationalMode == OperationalMode.CLOSING
        
    def test_type_conversions_long(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        tStamp = datetime.datetime.now(timezone.utc)
        msg = JsonMsg("MPoint-IDL", 97, IASType.LONG,tStamp,OperationalMode.DEGRADED)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-IDL"
        assert fromJString.value == 97
        assert fromJString.valueType == IASType.LONG
        assert fromJString.operationalMode == OperationalMode.DEGRADED
        
    def test_type_conversions_string(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDS", "A Test String", IASType.STRING, operationalMode=OperationalMode.OPERATIONAL)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-IDS"
        assert fromJString.value == "A Test String"
        assert fromJString.valueType == IASType.STRING
        assert fromJString.operationalMode == OperationalMode.OPERATIONAL
        
    def test_type_conversions_bool(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDB", True, IASType.BOOLEAN, operationalMode=OperationalMode.INITIALIZATION)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-IDB"
        assert fromJString.value == True
        assert fromJString.valueType == IASType.BOOLEAN
        assert fromJString.operationalMode == OperationalMode.INITIALIZATION
        
    def test_type_conversions_char(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDC", 'X', IASType.CHAR, operationalMode=OperationalMode.CLOSING)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-IDC"
        assert fromJString.value == 'X'
        assert fromJString.valueType == IASType.CHAR
        assert fromJString.operationalMode == OperationalMode.CLOSING
        
    def test_type_conversions_alarm(self):
        '''
        Test the conversion of all possible IAS data types
        '''
        msg = JsonMsg("MPoint-IDC", Alarm.get_initial_alarmstate(Priority.MEDIUM).set(), IASType.ALARM, operationalMode=OperationalMode.SHUTTEDDOWN)
        jStr = msg.dumps()
        print("testTypeConversionsAlarm JSON msg:", jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-IDC"
        assert fromJString.value == Alarm.get_initial_alarmstate(Priority.MEDIUM).set()
        assert fromJString.value.is_set()
        assert fromJString.value.priority == Priority.MEDIUM
        assert not fromJString.value.is_acked()
        assert fromJString.valueType == IASType.ALARM
        assert fromJString.operationalMode == OperationalMode.SHUTTEDDOWN

    def test_type_conversions_array_longs(self):
        msg = JsonMsg("MPoint-ARRAY-LONGS", [1,2,3,4,5,-1], IASType.ARRAYOFLONGS, operationalMode=OperationalMode.OPERATIONAL)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-ARRAY-LONGS"
        assert fromJString.value == [1,2,3,4,5,-1]
        assert fromJString.valueType == IASType.ARRAYOFLONGS
        assert fromJString.operationalMode == OperationalMode.OPERATIONAL

    def test_type_conversions_array_doubles(self):
        msg = JsonMsg("MPoint-ARRAY-DOUBLES", [0,0.123,-987.321,-1], IASType.ARRAYOFDOUBLES, operationalMode=OperationalMode.OPERATIONAL)
        jStr = msg.dumps()
        print(jStr)
        fromJString = JsonMsg.parse(jStr)
        assert fromJString.mPointID == "MPoint-ARRAY-DOUBLES"
        assert fromJString.value == [0,0.123,-987.321,-1]
        assert fromJString.valueType == IASType.ARRAYOFDOUBLES
        assert fromJString.operationalMode == OperationalMode.OPERATIONAL
