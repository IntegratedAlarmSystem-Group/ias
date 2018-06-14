#! /usr/bin/env python
'''

Test the kafka puiblisher and subscriber

Created on Jun 14, 2018

@author: acaproni
'''
import unittest
from IasSupport.KafkaValueConsumer import IasValueListener, KafkaValueConsumer
from IasSupport.KafkaValueProducer import KafkaValueProducer
from IasSupport.IasValue import IasValue
from IASLogging.logConf import Log

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
    
    kafkabrokers='localhost:9092'
    topic="Test-PyProdCons-Topic"
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
        iasValue.fullRunningId=frid
        iasValue.id=ident
        iasValue.value=str(value)
        return iasValue


    def testName(self):
        
        logger.info('Building the consumer')
        consumer = KafkaValueConsumer(
            self.listener,
            self.kafkabrokers,
            self.topic,
            'PyConsumerTest',
            'PyConsumerTestGroup')
        logger.info('Starting the consumer')
        consumer.start()
        
        logger.info('Building the producer')
        producer = KafkaValueProducer(self.kafkabrokers,self.topic,'PyProducerTest-ID')
        
        n=1000
        logger.info('Publishing %d IasValues',n)
        baseId='Test-ID#'
        for i in range(0,n):
            v = self.buildLONGValue(baseId+str(i),i)
            producer.send(v)
            
        producer.flush()
        logger.info('%d monitor pint sent',n)
        
        logger.info('Closing the producer')
        producer.close()
        logger.info('Producer closed')
        
        self.assertEqual(n, len(self.listener.receivedValues), 'Messages mismatch')


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    logger=Log.initLogging(__file__)
    logger.info("Start main")
    unittest.main()