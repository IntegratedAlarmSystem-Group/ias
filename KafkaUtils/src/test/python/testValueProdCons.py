#! /usr/bin/env python3
'''

Test the kafka publisher and subscriber

Created on Jun 14, 2018

@author: acaproni
'''
import time
import unittest

from IASLogging.logConf import Log
from IasBasicTypes.IasValue import IasValue
from IasKafkaUtils.KafkaValueConsumer import IasValueListener, KafkaValueConsumer
from IasKafkaUtils.KafkaValueProducer import KafkaValueProducer

logger = Log.getLogger(__file__)

class TestListener(IasValueListener):
    '''
    The listener of IasValues read from the kafka topic
    '''
    receivedValues = []
    
    def __init__(self):
        """
        Constructor
        
        """
        pass
    
    def iasValueReceived(self,iasValue):
        """
        Print the IasValue in the stdout
        """
        self.receivedValues.append(iasValue)


class TestValueProdCons(unittest.TestCase):
    
    kafkabrokers = 'localhost:9092'
    topic = "Test-PyProdCons-Topic"
    listener = TestListener()
    
    # JSON string to build IasValues
    jsonStr = """{"value":"0","pluginProductionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6","dasuProductionTStamp":"1970-01-01T00:00:00.7",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)","(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)"],
            "mode":"DEGRADED","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)",
            "valueType":"LONG"}"""
            
    fullRunningIdPrefix="(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@("
    fullRunningIdSuffix=":IASIO)"
    
    def buildLONGValue(self,ident, value):
        '''
        Builds a IAsValue
        
        @param ident: the identifier of the IasValue
        @param value: (LONG) the value
        '''
        iasValue = IasValue.fromJSon(self.jsonStr)
        frid = self.fullRunningIdPrefix+ident+self.fullRunningIdSuffix
        iasValue.fullRunningId = frid
        iasValue.id = ident
        iasValue.value = str(value)
        return iasValue


    def testName(self):

        logger.info('Building the producer')
        producer = KafkaValueProducer(self.kafkabrokers, self.topic, 'PyProducerTest-ID')
        
        logger.info('Building the consumer')
        consumer = KafkaValueConsumer(
            self.listener,
            self.kafkabrokers,
            self.topic,
            'PyConsumerTest',
            'PyConsumerTestGroup')
        logger.info('Starting the consumer')
        consumer.start()

        n=100

        # Wait one second to be sure the consumer is ready
        # Setting up partitions and assign consumers to group, rebalancing etc.
        # can slow down the process even if the consumer is ready and waiting
        # to get data
        time.sleep(1)
        logger.info('Publishing %d IasValues',n)
        baseId='Test-ID#'
        for i in range(0, n):
            v = self.buildLONGValue(baseId+str(i),i)
            producer.send(v)
            
        producer.flush()
        logger.info('%d monitor pint sent',n)
        
        logger.info('Closing the producer')
        producer.close()
        logger.info('Producer closed')

        # Wait some time if not all the items have been received
        timeout = 10.0 # seconds max waiting time
        slept = 0
        sleep_time = 0.25
        while (n != len(self.listener.receivedValues)) and (slept < timeout):
            time.sleep(sleep_time)
            slept = slept + sleep_time
        
        logger.info('Closing the consumer')
        consumer.close()
        logger.info('Consumer closed')
        
        self.assertEqual(n, len(self.listener.receivedValues), 'Messages mismatch')


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    logger.info("Start main")
    unittest.main()
