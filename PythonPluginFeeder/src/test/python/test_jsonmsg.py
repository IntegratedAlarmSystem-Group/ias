"""
The conversions operated by the JsonMsg
"""

from datetime import datetime
from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode

from IasPlugin3.JsonMsg import JsonMsg

class TestJsonMsg():

    def test_json_conversion(self):
        '''
        Test that the conversion to json and back to JsonMsg works correctly
        '''
        mPointID = "MPoint-ID"
        value = 2.3
        valueType = IASType.DOUBLE
        timestamp = datetime(2026, 3, 1, 11, 15, 7)
        operationalMode = OperationalMode.MAINTENANCE
        
        msg = JsonMsg(mPointID, value, valueType, timestamp, operationalMode)
        
        jsonStr = msg.dumps()
        
        parsedMsg = JsonMsg.parse(jsonStr)
        
        assert parsedMsg.mPointID == mPointID
        assert parsedMsg.value == value
        assert parsedMsg.valueType == valueType
        assert parsedMsg.timestamp == msg.timestamp
        assert parsedMsg.operationalMode == operationalMode
