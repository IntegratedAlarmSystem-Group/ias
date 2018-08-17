'''
Created on May 9, 2018

@author: acaproni
'''

import socket, os
from datetime import datetime
from IasPlugin2.JsonMsg import JsonMsg
import logging
from threading import Timer, RLock

class UdpPlugin(object):
    '''
    UpdPlugin for python 2 sends monitor points to the java plugin by means 
    of UDP sockets.
    
    Using UDP has pros and cons. Take into account that UDP
    is connectionless and as such does now warrant delivery
    neither the delivery order.
    On the other hand it is simple and fast (so fast that
    can saturate the network).
    
    UDP paradigm decouple the python code from the java plugin
    that is consistent with IAS design.
    Also the case of UDPs not delivered to the java plugin is
    consistent with IAS design: a missing monitor point will be marked
    as invalid until its value is refreshed.
    
    The java plugin is supposed to run in a server (hostname) and 
    be listening to UDP from the given port.
    
    
    Monitor points and alarm are not sent immediately but
    temporarily stored in a dictionary and sent at periodic
    time intervals.
    In this way if the same value is sent many times in the time interval 
    only the last value is effectively sent to the java plugin mitigating
    a misbehaving implementation. 
    '''
    
    # Monitor points are periodically sent in seconds
    SENDING_TIME_INTERVAL = 0.250
    
    # The operational mode
    mode = [ 'STARTUP',
            'INITIALIZATION',
            'CLOSING',
            'SHUTTEDDOWN', 
            'MAINTENANCE',
            'OPERATIONAL',
            'DEGRADED',
            'UNKNOWN']
    
    # The states of alarms
    alarm = [ 'SET_CRITICAL',
            'SET_HIGH',
            'SET_MEDIUM',
            'SET_LOW',
            'CLEARED']
    
    # The types of the monitor points
    valueType = ['LONG',
                 'INT',
                 'SHORT',
                 'BYTE',
                 'DOUBLE',
                 'FLOAT',
                 'BOOLEAN',
                 'CHAR',
                 'STRING',
                 'ALARM']
    
    def __init__(self, hostname, port):
        '''
        Constructor.
        
        @param hostname the host name to send data packets to
        @param port the port to send UDP packets to
        @raise exception: if the hostname is not resolved
        '''
        
        self._hostname = hostname
        self._port=port
        self._ip = socket.gethostbyname(self._hostname)
        
        logging.info('UdpPlugin will send UDP messages to %s(%s):%d',self._hostname,self._ip,self._port )
        
        # Monitor points to send are initially stored in the dictionary
        # (key=MPoint ID, value = JSonMsg)
        self._MPointsToSend = {}
        
        # The UDP socket to send messages to the java plugin
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        
        # A flag to terminate the thread when the
        # object is shut down
        self._shuttedDown = False
        
        # A flag reporting if the object has been initialized
        self._started = False
        
        # The time r to send monitor points to the 
        # java plugin
        self._timer = None
        
        # The lock for protecting shared data 
        # between threads
        self._lock = RLock()
        
        logging.info("UdpPlugin built")
    
    def start(self):
        '''
        Start the UdpPlugin
        '''
        assert  not self._started
        logging.info('Starting up')
        self._started = True
        self._timer = self._schedule()
        logging.info('Started.')
    
    def shutdown(self):
        '''
        Shutdown the plugin
        '''
        assert  not self._shuttedDown
        logging.info('Shutting down')
        self._lock.acquire(blocking=True)
        self._shuttedDown = True
        self._lock.release()
        if self._started and self._timer is not None:
            self._timer.cancel()
        self._sock.close()
        logging.info('Closed.')
        
    def _schedule(self):
        '''
        Schedule the timer for the next sending
        
        @return: the scheduled timer or None if shut down
        '''
        if not self._shuttedDown:
                self._lock.acquire()
                timer = Timer(UdpPlugin.SENDING_TIME_INTERVAL, self._sendMonitorPoints)
                timer.start()
                self._lock.release()
                return timer
        else:
            return None
        
    def submit(self, mPointID, value, valueType, timestamp=datetime.utcnow(), operationalMode='OPERATIONAL'):
        '''
        Submit a monitor point or alarm with the give ID to the java plugin.
        
        The monitor point is added to the dictionary and will be sent later
        
        @param mPointID: the not None nor empty ID of the monitor point
        @param value: the value of the monitor point
        @param valueType: the type of the monitor point (must be in self.valueType)
        @param timestamp: (datetime) the timestamp when the value has been
                          read from the monitored system
        @param operationalMode the optional operational mode must be in (self.mode)
        @see: JsonMsg.IAS_SUPPORTED_TYPES
        '''
        if not mPointID:
            raise ValueError("The ID can't be None neither empty")
        if timestamp is None:
            raise ValueError("The timestamp can't be None")
        if value is None:
            raise ValueError("The value can't be None")
        if not valueType:
            raise ValueError("The type can't be None")
        if self.valueType.count(valueType)==0:
            raise ValueError("Unrecognized type "+type)
        
        if not operationalMode:
            raise ValueError("The operational mode can't be None")
        if self.mode.count(operationalMode)==0:
            raise ValueError("Unrecognized operational mode "+operationalMode)
        
        if self._shuttedDown:
            return
        msg = JsonMsg(mPointID,value, valueType,timestamp,operationalMode)
        self._lock.acquire()
        self._MPointsToSend[msg.mPointID]=msg
        self._lock.release()
        logging.debug("Monitor point %s of type %s submitted with value %s and mode %s (%d values in queue)",
                          msg.mPointID,
                          msg.valueType,
                          msg.value,
                          msg.operationalMode,
                          len(self._MPointsToSend))
        
    def _sendMonitorPoints(self):
        '''
        The periodic task that send monitor points to the java plugin
        through the UDP socket
        '''
        if not self._shuttedDown:
            logging.debug("Sending %d monitor points",len(self._MPointsToSend))
            self._lock.acquire()
            valuesToSend = list(self._MPointsToSend.values())
            self._MPointsToSend.clear()
            self._lock.release()
            #
            # Send the monitor points with the UDP socket
            #
            for mPoint in valuesToSend:
                self._send(mPoint)
            logging.debug('Monitor points sent')
            del valuesToSend[:]
            
            ## reschedule the time if not closed
            self._timer = self._schedule()
            
    
    def _send(self, mPoint):
        ''' 
        Send the passed monitor points to the java plugin through the UDP socket
        
        @param mPoint: the monitor point (JsonMsg) to send to the java plugin 
        '''
        # Get the JSON representation of the object to send
        jsonStr = mPoint.dumps()
        
        # send the string to the UDP socket
        self._sock.sendto(jsonStr.encode("utf-8"),(self._ip, self._port))
        
