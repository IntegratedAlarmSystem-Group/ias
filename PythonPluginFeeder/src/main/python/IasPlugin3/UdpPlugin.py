'''
Created on May 9, 2018

@author: acaproni
'''

import socket
from datetime import datetime
from IasPlugin3.JsonMsg import JsonMsg
import logging
import time
from threading import Thread, Lock

class UdpPlugin():
    '''
    UpdPlugin sends monitor points to the java plugin by means 
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

        # The logger
        self.logger = logging.getLogger(UdpPlugin.__name__)
        
        self.logger.info('UdpPlugin will send UDP messages to %s(%s):%d',self._hostname,self._ip,self._port )
        
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
        
        # The lock to protect critical sections
        self._lock: Lock = Lock()

        self.thread: Thread = Thread(
            target = self._sendMonitorPoints,
            name="send_mppoints_thread",
            daemon=True
        )
        
        self.logger.info("UdpPlugin built")
    
    def start(self):
        '''
        Start the UdpPlugin
        '''
        assert  not self._started
        self.logger.info('Starting up')
        self._started = True
        self.thread.start()
        self.logger.info('Started.')
    
    def shutdown(self):
        '''
        Shutdown the plugin
        '''
        assert  not self._shuttedDown
        self.logger.info('Shutting down')
        with self._lock:
            if len(self._MPointsToSend.values()) > 0:
                self.logger.warning("There are still %d monitor points to send. They will be lost", len(self._MPointsToSend.values()))
                self._MPointsToSend.clear()
            self._shuttedDown = True
        
            self._sock.close()
            self.logger.debug("UDP socket closed")

        self.logger.info('Closed.')
        
    def submit(self, mPointID, value, valueType, timestamp: datetime|None=None, operationalMode=None):
        '''
        Submit a monitor point or alarm with the give ID to the java plugin.
        
        The monitor point is added to the dictionary and will be sent later
        
        @param mPointID: the not None nor empty ID of the monitor point
        @param value: the value of the monitor point
        @param valueType: (IasTye)the IasType of the monitor point
        @param timestamp: (datetime) the timestamp when the value has been
                          read from the monitored system or None to set the timestamp 
                          to the actual time
        @param operationalMode (OperationalMode) the optional operational mode
        @see: JsonMsg.IAS_SUPPORTED_TYPES
        '''
        if not mPointID:
            raise ValueError("The ID can't be None neither empty")
        if value is None:
            raise ValueError("The value can't be None")
        if valueType is None:
            raise ValueError("The type can't be None")
        
        msg = JsonMsg(mPointID, value, valueType, timestamp, operationalMode)
        with self._lock:
            if self._shuttedDown:
                self.logger.warning("The plugin is shutted down. Monitor point %s will not be sent", mPointID)
                return
            self._MPointsToSend[msg.mPointID]=msg
        
        self.logger.debug("Monitor point %s of type %s submitted with value %s and mode %s (%d values in queue)",
                          msg.mPointID,
                          msg.valueType,
                          msg.value,
                          msg.operationalMode,
                          len(self._MPointsToSend))
        
    def _sendMonitorPoints(self):
        '''
        The thread that sends monitor points to the java plugin
        through the UDP socket
        '''
        while not self._shuttedDown:
            self.logger.debug("Sending collected monitor points")
            self.flush()
            time.sleep(UdpPlugin.SENDING_TIME_INTERVAL)
        self.logger.debug("thread to send monitor points terminated")

    def flush(self):
        '''
        Flush the monitor points to send.
        '''
        with self._lock:
            valuesToSend = list(self._MPointsToSend.values())
            self._MPointsToSend.clear()
            if self._shuttedDown:
                if len(valuesToSend) > 0:
                    self.logger.warning("The plugin is shutted down. %d monitor points will not be sent", len(valuesToSend))
                return
        
        if len(valuesToSend) == 0:
            self.logger.debug("No monitor points to send")
            return
        
        self.logger.debug("Flushing %d monitor points",len(valuesToSend))
        #
        # Send the monitor points with the UDP socket
        #
        for mPoint in valuesToSend:
            self._send(mPoint)
        self.logger.debug('Monitor points flushed')
        valuesToSend.clear()
    
    def _send(self, mPoint):
        ''' 
        Send the passed monitor points to the java plugin through the UDP socket
        
        @param mPoint: the monitor point (JsonMsg) to send to the java plugin 
        '''
        # Get the JSON representation of the object to send
        jsonStr = mPoint.dumps()
        
        # send the string to the UDP socket
        with self._lock:
            if not self._shuttedDown:
                self._sock.sendto(bytes(jsonStr, "utf-8"),(self._ip, self._port))
        