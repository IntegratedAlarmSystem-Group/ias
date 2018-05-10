#! /usr/bin/env python
'''
Test for UdpPlugin

Created on May 9, 2018

@author: acaproni
'''
import unittest
import socket
import time
from threading import Thread
from IasPlugin.UdpPlugin import UdpPlugin
from IasPlugin.JsonMsg import JsonMsg
from IasPlugin.OperationalMode import OperationalMode
from IasPlugin.IasType import IASType

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
            

class TestUdpPlugin(unittest.TestCase):
    
    HOST = 'localhost'
    PORT = 10001
    
    def setUp(self):
        self.receiver = MessageReceiver()
        self.receiver.setUp()
        self.plugin = UdpPlugin(TestUdpPlugin.HOST, TestUdpPlugin.PORT)
        
        
    def tearDown(self):
        self.plugin.shutdown()
        self.receiver.tearDown()
        ## Give time to close the UDP socket before next iteration
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL) 
    
    def testDoesNotSendIfNotStarted(self):
        '''
        Test if the plugin send nothing before being started
        '''
        self.plugin.submit("MPoint-ID", 123, IASType.INT)
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL)
        self.assertEqual(len(self.receiver.msgReceived),0)
        
    def testSendIfStarted(self):
        '''
        Test that the plugin send a monitor point to the UDP after 
        being started
        '''
        self.plugin.start()
        self.plugin.submit("MPoint-ID", 123, IASType.INT)
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL)
        self.assertEqual(len(self.receiver.msgReceived),1)
        
    def testSentValue(self):
        '''
        Test that the plugin effectively sent what has been submitted
        '''
        self.plugin.start()
        self.plugin.submit("MPoint-ID", 2.3, IASType.DOUBLE)
        time.sleep(2*UdpPlugin.SENDING_TIME_INTERVAL)
        self.assertEqual(len(self.receiver.msgReceived),1)
        msg = JsonMsg.parse(self.receiver.msgReceived[0])
        self.assertEqual(msg.identifier,"MPoint-ID")
        self.assertEqual(msg.value,str(2.3))
        self.assertEqual(msg.valueType,IASType.DOUBLE)
        

if __name__ == '__main__':
    unittest.main()

