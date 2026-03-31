#! /usr/bin/env python3
'''
A plugin that sends some monitor points through the UDP
socket to the java receiver.

The scope of this plugin is to close the loop from python
to the java plugin that pushes values in the BSDB.

Created on May 10, 2018

@author: acaproni
'''
import time
import logging

from IasLogging.log import Log
from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.Priority import Priority
from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode
from IasPlugin3.UdpPlugin import UdpPlugin

if __name__ == '__main__':
    Log.init_logging(__file__, 'debug', 'debug')
    logger = logging.getLogger("MockUdpPlugin")
    udpPlugin = UdpPlugin("localhost",10101)
    udpPlugin.start()
    logger.info("Mock UDP plugin started")
    time.sleep(1)

    logger.debug("Submitting ID-Double")
    udpPlugin.submit("ID-Double", 122.54, IASType.DOUBLE, operationalMode=OperationalMode.INITIALIZATION)

    logger.debug("Submitting ID-Long")
    udpPlugin.submit("ID-Long", 1234567, IASType.INT, operationalMode=OperationalMode.STARTUP)

    logger.debug("Submitting ID-Bool")
    udpPlugin.submit(mPointID="ID-Bool", value=False, valueType=IASType.BOOLEAN, operationalMode=OperationalMode.OPERATIONAL)

    logger.debug("Submitting ID-Char")
    udpPlugin.submit("ID-Char", 'X', IASType.CHAR, operationalMode=OperationalMode.DEGRADED)

    logger.debug("Submitting ID-String")
    udpPlugin.submit("ID-String", 'Testing for test', IASType.STRING, operationalMode=OperationalMode.CLOSING)

    logger.debug("Submitting ID-Alarm")
    udpPlugin.submit("ID-Alarm", Alarm.get_initial_alarmstate(Priority.HIGH).set(), IASType.ALARM)

    logger.debug("Submitting ID-ArrayLong")
    udpPlugin.submit("ID-ArrayLong", [-1,5,10,0], IASType.ARRAYOFLONGS)

    logger.debug("Submitting ID-ArrayDouble")
    udpPlugin.submit("ID-ArrayDouble", [-123,0.654,7], IASType.ARRAYOFDOUBLES)
 
    udpPlugin.flush()
    logger.info("All monitor points submitted")
    
    logger.debug("Shutting down the plugin")
    udpPlugin.shutdown()
    logger.info("Plugin shutted down")
    
    
