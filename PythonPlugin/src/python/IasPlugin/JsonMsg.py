'''
Created on May 9, 2018

@author: acaproni
'''

import json
from datetime import datetime

class JsonMsg(object):
    
    '''
    This class encapsulate the field of the message to send
    to the java plugin as JSON string
    
    It is the  counter part of the java org.eso.ias.plugin.network.MessageDao
    '''

    def __init__(self, id, timestamp=datetime.utcnow(), value, valueType):
        '''
        Constructor
        
        @param id: the identifier of the monitor point
        @param timestamp: (datetime) the point in time when the value 
                          has been read from the monitored system; 
                          if not provided, it is set to the actual time
        @param value: (string)  the value to send to the BSDB
        @param valueType: the type of the value
        '''
        self.id=str(id)
        if not self.id:
            raise ValueError("Invalid empty ID") 
        
        if not isinstance(timestamp, datetime):
            raise ValueError("The timestamp should be of type datetime (UCT)") 
        self.timestamp=self.isoTimestamp(timestamp)
        
        self.value=str(value)
        if not self.value:
            raise ValueError("Invalid empty value")
       
        self.valueType=str(valueType)
        if not self.valueType:
            raise ValueError("Invalid empty type")
        
        # define the names of the json fields
        self.idJsonParamName = "id"
        self.tStampJsonParamName = "timestamp"
        self.valueJsonParamName = "value"
        self.valueTypeJsonParamName = "valueType"
        
    def isoTimestamp(self, tStamp):
        '''
        Format the passed timestamp as ISO.
        
        The formatting is needed because datetime is formatted 
        contains microseconds but we use milliseconds precision
        
        @param the datetime to transfor as ISO 8601
        '''
        
        # isoTStamp in microseconds like 2018-05-09T16:15:05.775444
        isoTStamp=self.timestamp.isoformat() 
        
        # split the timestamop in 2 parts like
        # ['2018-05-09T16:15:05', '775444']
        splittedTStamp = isoTStamp.split('.')
        milliseconds = splittedTStamp[1][:3]
        return splittedTStamp[0]+"."+milliseconds
        
    def dumps(self):
        '''
        Return the JSON string to send to the java plugin 
        '''
        return json.dumps({
            self.idJsonParamNam:self.id, 
            self.tStampJsonParamName:self.timestamp, 
            self.valueJsonParamName: self.value, 
            self.valueTypeJsonParamName:self.valueType})
    
    def parse(self,jsonStr):
        '''
        Parse the passed string and return a JsonMsg
        
        @param jsonStr the json string representing a message to send to the java plugin
        '''
        obj = json.loads(jsonStr)
        return JsonMsg(
            obj[self.idJsonParamNam],
            obj[self.tStampJsonParamName],
            obj[self.valueJsonParamName],
            obj[self.valueTypeJsonParamName])
        