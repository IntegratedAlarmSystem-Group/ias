#! /usr/bin/env python
'''
A plugin that sends some monitor points through the UDP
socket to the java receiver.

The scope of this plugin is to close the loop from python
to the java plugin that pushes values in the BSDB.

Created on May 10, 2018

@author: acaproni
'''
import time
from IasPlugin3.UdpPlugin import UdpPlugin
from IasPlugin3.IasType import IASType
from IasPlugin3.OperationalMode import OperationalMode
from IasPlugin3.Alarm import Alarm

if __name__ == '__main__':
    udpPlugin = UdpPlugin("localhost",10101)
    udpPlugin.start()
    time.sleep(1)
    udpPlugin.submit("ID-Double", 122.54, IASType.DOUBLE, operationalMode=OperationalMode.INITIALIZATION)
    udpPlugin.submit("ID-Long", 1234567, IASType.INT, operationalMode=OperationalMode.STARTUP)
    udpPlugin.submit("ID-Bool", False, IASType.BOOLEAN, operationalMode=OperationalMode.OPERATIONAL)
    udpPlugin.submit("ID-Char", 'X', IASType.CHAR, operationalMode=OperationalMode.DEGRADED)
    udpPlugin.submit("ID-String", 'Testing for test', IASType.STRING, operationalMode=OperationalMode.CLOSING)
    udpPlugin.submit("ID-Alarm", Alarm.SET_HIGH, IASType.ALARM)
    
    time.sleep(.5)
    udpPlugin.shutdown()
    
    