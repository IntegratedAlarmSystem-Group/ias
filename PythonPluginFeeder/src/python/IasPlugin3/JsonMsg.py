'''
Created on May 9, 2018

@author: acaproni
'''

import json
from datetime import datetime
import dateutil.parser
from IasPlugin3.OperationalMode import OperationalMode
from IasPlugin3.IasType import IASType

class JsonMsg(object):
    '''
    This class encapsulate the field of the message to send
    to the java plugin as JSON string
    
    It is the  counter part of the java org.eso.ias.plugin.network.MessageDao
    
    The type must be one of types supported by the IAS as defined in
    org.eso.ias.types.IASTypes
    '''
    
    # define the names of the json fields
    idJsonParamName = "monitorPointId"
    tStampJsonParamName = "timestamp"
    valueJsonParamName = "value"
    valueTypeJsonParamName = "valueType"
    operationaModeParamName = "operMode"
        
    def __init__(self, mpointId, value, valueType, timestamp=datetime.utcnow(), operationalMode=None):
        '''
        Constructor
        
        @param mpointId: the mpointId of the monitor point
        @param value: the value to send to the BSDB
        @param valueType: (IASType) the type of the value
        @param timestamp: (datetime) the point in time when the value 
                          has been read from the monitored system; 
                          if not provided, it is set to the actual time
                          It can be either datetime or a ISO-8601 string
        @param operationalMode: (OperationalMode) optionally sets a operational mode 
                                for the passed monitor point
        '''
        
        
        if not mpointId:
            raise ValueError("Invalid empty ID")
        self.mPointID=str(mpointId) 
        
        if timestamp is None:
            raise ValueError("The timestamp can't be None")
        
        if not isinstance(timestamp, datetime):
            # Is it a ISO 8601 string?
            temp = str(timestamp)
            if (self.checkStringIsoTimestamp(temp)):
                self.timestamp=temp
            else:
                raise ValueError("The timestamp must be of type datetime (UCT) or an ISO 8601 string")
        else: 
            self.timestamp=self.isoTimestamp(timestamp)
        
        if value is None or value=="":
            raise ValueError("Invalid empty value")
        self.value=value
       
        if not isinstance(valueType, IASType):
            raise ValueError("Invalid type: "+valueType)
        self.valueType=valueType
        
        if not operationalMode is None and not isinstance(operationalMode,OperationalMode):
            raise ValueError("Invalid operational mode "+operationalMode)
        self.operationalMode = operationalMode
    
    
    def checkStringIsoTimestamp(self,tStamp):
        '''
        Check if the passed string represent a ISO timestamp
        
        @param tStamp the string representing a ISO timestamp
        @return True if tStamp is a ISO 8601 timestamp,
                False otherwise
        '''
        try:
            dateutil.parser.parse(tStamp)
            return True
        except:
            return False
            
        
    def isoTimestamp(self, tStamp):
        '''
        Format the passed timestamp as ISO.
        
        The formatting is needed because datetime is formatted 
        contains microseconds but we use milliseconds precision
        
        @param the datetime to transfor as ISO 8601
        '''
        
        # isoTStamp in microseconds like 2018-05-09T16:15:05.775444
        isoTStamp=tStamp.isoformat() 
        
        # split the timestamop in 2 parts like
        # ['2018-05-09T16:15:05', '775444']
        splittedTStamp = isoTStamp.split('.')
        milliseconds = splittedTStamp[1][:3]
        return splittedTStamp[0]+"."+milliseconds
        
    def dumps(self):
        '''
        Return the JSON string to send to the java plugin 
        '''
        
        vType = str(self.valueType).split('.')[1]
        
        if self.operationalMode is None:
            mode = ""
        else:
            mode = str(self.operationalMode).split('.')[1]
        
        if self.valueType==IASType.ALARM:
            value = str(self.value).split(".")[1]
        else:
            value = str(self.value)
        
        return json.dumps({
            JsonMsg.idJsonParamName:self.mPointID, 
            JsonMsg.tStampJsonParamName:self.timestamp, 
            JsonMsg.valueJsonParamName: value, 
            JsonMsg.valueTypeJsonParamName: vType,
            JsonMsg.operationaModeParamName: mode})
    
    
    @staticmethod
    def parse(jsonStr):
        '''
        Parse the passed string and return a JsonMsg
        
        @param jsonStr the json string representing a message to send to the java plugin
        '''
        obj = json.loads(jsonStr)
        
        mPointType = IASType.fromString(obj[JsonMsg.valueTypeJsonParamName])
        
        value = mPointType.convertStrToValue(obj[JsonMsg.valueJsonParamName])
        
        if JsonMsg.operationaModeParamName in obj and not obj[JsonMsg.operationaModeParamName]=="":
            mode = OperationalMode.fromString(obj[JsonMsg.operationaModeParamName])
        else:
            mode = None
        
        return JsonMsg(
            obj[JsonMsg.idJsonParamName],
            value,
            mPointType,
            obj[JsonMsg.tStampJsonParamName],
            mode)
        