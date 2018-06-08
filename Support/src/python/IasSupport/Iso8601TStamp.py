'''
Created on Jun 8, 2018

@author: acaproni
'''
from datetime import datetime

class Iso8601TStamp(object):
    '''
    Helper for converting from datetime to ISO 8601
    and vice-versa
    '''

    @staticmethod
    def datetimeToIsoTimestamp(tStamp):
        '''
        Format the passed (datetime) timestamp as ISO.
        
        The formatting is needed because datetime is formatted 
        contains microseconds but we use milliseconds precision
        
        @param the datetime to convert as ISO 8601
        @return the ISO timestamp
        '''
        
        # isoTStamp in microseconds like 2018-05-09T16:15:05.775444
        isoTStamp=tStamp.isoformat() 
        
        # split the timestamop in 2 parts like
        # ['2018-05-09T16:15:05', '775444']
        splittedTStamp = isoTStamp.split('.')
        milliseconds = splittedTStamp[1][:3]
        return splittedTStamp[0]+"."+milliseconds
    
    @staticmethod
    def Iso8601ToDatetime(iso8601TStamp):
        """
        Convert the passed ISO 8601 timestamo 
        to a datetime
        @param iso8601TStamp the ISO 8601 timestamp
        @return the datetime representing the passed timestamp
        """
        splitByT = iso8601TStamp.split("T")
        date = splitByT[0].split("-")
        year = int(date[0])
        month = int(date[1])
        day = int(date[2])
        
        time = splitByT[1].split(":")
        hour = int(time[0])
        mins = int(time[1])
        sec = int(time[2].split(".")[0])
        micorsec = int(time[2].split(".")[1])*1000
        
        return datetime(year,month,day,hour,mins,sec,micorsec)
    
    @staticmethod
    def now():
        """
        @return The ISO timestamp of the actual time
        """
        timestamp=datetime.utcnow()
        return Iso8601TStamp.datetimeToIsoTimestamp(timestamp)
    