'''
Created on May 9, 2018

@author: acaproni
'''

import json
from datetime import datetime
from datetime import timezone
import dateutil.parser
import logging

from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode


class JsonMsg(object):
    '''
    Objects of this class encapsulate the field of the message to send
    to the java plugin as JSON string
    
    It is the counter part of the java org.eso.ias.plugin.network.MessageDao
    
    The type must be one of types supported by the IAS as defined in
    org.eso.ias.types.IASTypes
    '''
    
    # define the names of the json fields
    idJsonParamName = "monitorPointId"
    tStampJsonParamName = "timestamp"
    valueJsonParamName = "value"
    valueTypeJsonParamName = "valueType"
    operationaModeParamName = "operMode"

    logger = logging.getLogger(__name__)
        
    def __init__(self, mpointId: str, value, valueType: IASType, timestamp: datetime|None=None, operationalMode:OperationalMode|None=None):
        '''
        Constructor
        
        @param mpointId: the mpointId of the monitor point
        @param value: the value to send to the BSDB
        @param valueType: (IASType) the type of the value
        @param timestamp: (datetime) the point in time when the value 
                          has been read from the monitored system; 
                          if None, the timestamp is set to the actual time
        @param operationalMode: (OperationalMode) optionally sets a operational mode 
                                for the passed monitor point
        '''
        JsonMsg.logger.debug("Creating JsonMsg with mPointId %s, value %s, valueType %s, timestamp %s and operational mode %s",
                             mpointId, value, valueType, timestamp, operationalMode)
        
        if not mpointId:
            raise ValueError("Invalid empty ID")
        self.mPointID=str(mpointId) 
        
        if timestamp is None:
            timestamp = datetime.now(timezone.utc)
        
        if not isinstance(timestamp, datetime):
            raise ValueError("The timestamp must be of type datetime (UCT) or None")
        
        self.timestamp: str = self.isoTimestamp(timestamp)
        if value is None:
            raise ValueError("Invalid empty value")
        self.value=value
       
        if valueType is None:
            raise ValueError("The type can't be None")
        self.valueType: IASType =valueType
        JsonMsg.logger.debug("3")
        if not operationalMode is None and not isinstance(operationalMode,OperationalMode):
            raise ValueError("Invalid operational mode "+operationalMode)
        self.operationalMode = operationalMode

        JsonMsg.logger.debug("JsonMsg created with mPointId %s, value %s, valueType %s, timestamp %s and operational mode %s",
                             self.mPointID, self.value, self.valueType, self.timestamp, self.operationalMode)
        
    def isoTimestamp(self, tStamp: datetime) -> str:
        '''
        Format the passed timestamp as ISO.
        
        The formatting is needed because datetime  
        contains microseconds while ISO 8601 has milliseconds precision
        
        @param the datetime to convert to ISO 8601
        @return The ISO 8601 string representation of the passed datetime
        '''
        
        # isoTStamp in microseconds like 2018-05-09T16:15:05.775444
        isoTStamp=tStamp.isoformat() 
        
        # split the timestamp in 2 parts like
        # ['2018-05-09T16:15:05', '775444']
        if not '.' in isoTStamp:
            isoTStamp=isoTStamp+".000000"
        splittedTStamp = isoTStamp.split('.')
        milliseconds = splittedTStamp[1][:3]
        return splittedTStamp[0]+"."+milliseconds
        
    def dumps(self) -> str:
        '''
        Return the JSON string to send to the java plugin 
        '''
        vType = str(self.valueType)
        
        if self.operationalMode is None:
            mode = ""
        else:
            mode = str(self.operationalMode).split('.')[1]
        
        if self.valueType==IASType.ALARM:
            value = self.value.to_string()
        elif self.valueType==IASType.ARRAYOFLONGS or self.valueType==IASType.ARRAYOFDOUBLES:
            value = str(self.value)
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

        tstamp = dateutil.parser.isoparse(obj[JsonMsg.tStampJsonParamName])
        
        return JsonMsg(
            obj[JsonMsg.idJsonParamName],
            value,
            mPointType,
            tstamp,
            mode)
        