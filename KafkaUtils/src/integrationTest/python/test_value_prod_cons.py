"""
Test the kafka publisher and subscriber.

Uses IasLogConsumer directly since the test publishes to an arbitrary topic
rather than the core topic.

Created on Jun 14, 2018

@author: acaproni
"""
import time
import logging
from threading import Event
import pytest

from IasLogging.log import Log
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener
from IasKafkaUtils.KafkaValueProducer import KafkaValueProducer


class ValueLogListener(IasLogListener):
    """
    Listener that decodes JSON logs into IasValue objects and stores them
    for later verification.
    """

    def __init__(self):
        self._logger = logging.getLogger("TestValueProdCons.TestListener")
        self.receivedValues = []

    def iasLogReceived(self, log: str) -> None:
        try:
            iasValue = IasValue.fromJSon(log)
            from datetime import datetime, timezone
            iasValue.readFromBsdbTStamp = datetime.now(timezone.utc)
            iasValue.readFromBsdbTStampStr = Iso8601TStamp.datetimeToIsoTimestamp(
                iasValue.readFromBsdbTStamp
            )
            self.receivedValues.append(iasValue)
            self._logger.info("Value received %s", str(iasValue.value))
        except Exception:
            self._logger.exception("Exception parsing IasValue from log [%s]", log)


class TestValueProdCons():
    
    kafkabrokers = IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS
    topic = "Test-PyProdCons-Topic"
    
    # JSON string to build IasValues
    jsonStr = """{"value":"0","pluginProductionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6","productionTStamp":"1970-01-01T00:00:00.7",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)","(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)"],
            "mode":"DEGRADED","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)",
            "valueType":"LONG"}"""
            
    fullRunningIdPrefix="(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@("
    fullRunningIdSuffix=":IASIO)"

    @pytest.fixture(autouse=True)
    def setup_every_test(self):
        self.listener = ValueLogListener()

    @classmethod
    def setup_class(cls):
        Log.init_logging(__file__)
        cls.LOGGER = logging.getLogger(cls.__name__)
    
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
    
    def test_name(self):

        TestValueProdCons.LOGGER.info('Building the producer')
        producer = KafkaValueProducer(self.kafkabrokers, self.topic, 'PyProducerTest-ID')
        
        TestValueProdCons.LOGGER.info('Building the consumer')
        consumer = IasLogConsumer(
            self.listener,
            self.kafkabrokers,
            self.topic,
            'PyConsumerTest',
            'PyConsumerTestGroup')
        TestValueProdCons.LOGGER.info('Starting the consumer')
        consumer_ready = Event()
        consumer.start(ready_event=consumer_ready)
        assert consumer_ready.wait(timeout=30), "Consumer failed to subscribe within 30s"

        n=100
        
        TestValueProdCons.LOGGER.info('Publishing %d IasValues',n)
        baseId='Test-ID#'
        for i in range(0, n):
            v = self.buildLONGValue(baseId+str(i),i)
            producer.send(v)
            
        producer.flush()
        TestValueProdCons.LOGGER.info('%d monitor point sent',n)
        
        TestValueProdCons.LOGGER.info('Closing the producer')
        producer.close()
        TestValueProdCons.LOGGER.info('Producer closed')

        # Wait until all items have been received or timeout expires
        timeout = 30.0 # seconds max waiting time
        slept = 0
        sleep_time = 0.25
        while (n != len(self.listener.receivedValues)) and (slept < timeout):
            time.sleep(sleep_time)
            slept = slept + sleep_time

        TestValueProdCons.LOGGER.info('Closing the consumer')
        consumer.close()
        TestValueProdCons.LOGGER.info('Consumer closed')

        assert n == len(self.listener.receivedValues), f'Only received {len(self.listener.receivedValues)} out of {n}'
