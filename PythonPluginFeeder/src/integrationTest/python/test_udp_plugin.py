'''
Test for UdpPlugin

Created on May 9, 2018

@author: acaproni
'''
import socket
import time
from threading import Thread
from IasPlugin3.UdpPlugin import UdpPlugin
from IasPlugin3.JsonMsg import JsonMsg
from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.IasType import IASType
from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.Priority import Priority

class MessageReceiver(Thread):
    
    def __init__(self):
        self.closed = False
        Thread.__init__(self,name='Udp sender thread',daemon=True)
    
    def setUp(self):
        
        self._recvSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self._recvSocket.bind(('', TestUdpPlugin.PORT))
        self._recvSocket.settimeout(0.2)
        self.msgReceived = []
        self.start()
        
    def tearDown(self):
        self.closed = True
        self._recvSocket.close()
        self.msgReceived
    
    def run(self):
        print("Receiver thread running")
        while not self.closed:
            try:
                message, address = self._recvSocket.recvfrom(1024)
            except:
                continue
            jsonMsg = message.decode("utf-8")
            self.msgReceived.append(jsonMsg)
            print("Message received: "+jsonMsg) 
            

class TestUdpPlugin():
    
    HOST = 'localhost'
    PORT = 10001
    
    def setup_method(self):
        self.receiver = MessageReceiver()
        self.receiver.setUp()
        self.plugin = UdpPlugin(TestUdpPlugin.HOST, TestUdpPlugin.PORT)
        
        
    def teardown_method(self):
        self.plugin.shutdown()
        self.receiver.tearDown()
        ## Give time to close the UDP socket before next iteration
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL) 
    
    def test_does_not_send_if_not_started(self):
        '''
        Test if the plugin send nothing before being started
        '''
        self.plugin.submit("MPoint-ID", 123, IASType.INT)
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL)
        assert len(self.receiver.msgReceived) == 0
        
    def test_send_if_started(self):
        '''
        Test that the plugin send a monitor point to the UDP after 
        being started
        '''
        self.plugin.start()
        self.plugin.submit("MPoint-ID", 123, IASType.INT)
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL)
        assert len(self.receiver.msgReceived) == 1
        
    def test_sent_value(self):
        '''
        Test that the plugin effectively sent what has been submitted
        '''
        self.plugin.start()
        self.plugin.submit("MPoint-ID", 2.3, IASType.DOUBLE)
        self.plugin.submit("MPoint-IDOpMode", 5, IASType.INT,operationalMode=OperationalMode.MAINTENANCE)
        self.plugin.submit("MPoint-Alarm", Alarm.get_initial_alarmstate(Priority.CRITICAL).set(), IASType.ALARM,operationalMode=OperationalMode.DEGRADED)
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL)
        assert len(self.receiver.msgReceived) == 3
        dict = {}
        for jmsg in self.receiver.msgReceived:
            msg = JsonMsg.parse(jmsg)
            dict[msg.mPointID]=msg
            
        
        m = dict["MPoint-ID"]
        assert m.mPointID == "MPoint-ID"
        assert m.value == 2.3
        assert m.valueType == IASType.DOUBLE
        assert m.operationalMode is None
        
        m = dict["MPoint-IDOpMode"]
        assert m.mPointID == "MPoint-IDOpMode"
        assert m.value == 5
        assert m.valueType == IASType.INT
        assert m.operationalMode == OperationalMode.MAINTENANCE
        
        m = dict["MPoint-Alarm"]
        assert m.mPointID == "MPoint-Alarm"
        assert m.value == Alarm.get_initial_alarmstate(Priority.CRITICAL).set()
        assert m.valueType == IASType.ALARM
        assert m.operationalMode == OperationalMode.DEGRADED
