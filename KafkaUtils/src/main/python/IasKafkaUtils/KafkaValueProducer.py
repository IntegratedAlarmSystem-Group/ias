'''
Created on Jun 14, 2018

@author: acaproni
'''
import logging
from kafka import KafkaProducer

logger = logging.getLogger(__file__)

class KafkaValueProducer(object):
    '''
    KafkaValueProducer publishes IaValues in a kafka topic
    '''

    # The kafka producer
    producer = None
    
    # The kafka topic
    topic = None

    def __init__(self, 
                 kafkabrokers,
                 topic,
                 clientid):
        '''
        Constructor
        
        @param kafkabroker: the kafka servers to connect to
        @param: the kafka topic to publish IasValues
        @param clientId: the id of the kafka client
        '''
        if not topic:
            raise ValueError('Invalid empty topic name')
        
        logger.info("Building kafka producer to connect to %s, with topic %s and id %s",
                    kafkabrokers,topic,clientid)
        
        self.producer = KafkaProducer(
            bootstrap_servers=kafkabrokers, 
            client_id=clientid,
            linger_ms=100,
            key_serializer=str.encode,
            value_serializer=str.encode)
        if type(topic) == bytes:
            self.topic = topic.decode('utf-8')
        else:
            self.topic=topic
            
        logger.info("kafka producer connected to %s, with id %s",
                    kafkabrokers,clientid)
        
    def send(self,iasValue):
        '''
        Async send a IasValue to the kafka topic
        
        @param iasValue: the IasValue to publish
        @return the feature to be informed when the value has been sent
        '''
        iasValueStr = value=iasValue.toJSonString()
        id = iasValue.id
        return self.producer.send(self.topic,value=iasValueStr,key=id)
        
    def flush(self):
        '''
        Flush: delegates to the kafka producer
        '''
        self.producer.flush()
        
    def close(self):
        '''
        Close the producer: delegates to the kafka producer
        '''
        self.producer.close(2000)
        
        