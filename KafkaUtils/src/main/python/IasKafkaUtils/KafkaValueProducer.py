'''
Created on Jun 14, 2018

@author: acaproni
'''
import logging
from confluent_kafka import Producer

class KafkaValueProducer(object):
    '''
    KafkaValueProducer publishes IaValues in a kafka topic
    '''
    
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
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logger.info("Building kafka producer to connect to %s, with topic %s and id %s",
                    kafkabrokers, topic, clientid)

        conf = { 
            'bootstrap.servers': kafkabrokers, 
            'client.id': clientid, 
            'acks': 'all',
            "enable.idempotence": True,}

        # The kafka producer
        self.producer = Producer(conf)

        # The kafka topic
        if type(topic) == bytes:
            self.topic = topic.decode('utf-8')
        else:
            self.topic=topic
            
        self._logger.info("kafka producer connected to %s, with id %s",
                    kafkabrokers,clientid)
        
    def send(self, iasValue) -> None:
        '''
        Send a IasValue to the kafka topic
        
        @param iasValue: the IasValue to publish
        '''
        iasValueStr = iasValue.toJSonString()
        id = iasValue.id
        self.producer.produce(self.topic, value=iasValueStr, key=id, callback=self.delivery_report)
        self.producer.flush()

    def delivery_report(self, err, msg):
        '''
        Callback for the delivery report of a message sent to kafka
        
        @param err: the error if any
        @param msg: the message sent
        '''
        if err is not None:
            self._logger.error('IasValue delivery failed: %s', err)
        else:
            self._logger.debug('IasValue delivered to %s [%d] at offset %d',
                        msg.topic(), msg.partition(), msg.offset())
        
    def flush(self):
        '''
        Flush: delegates to the kafka producer
        '''
        self.producer.flush()
        
    def close(self):
        '''
        Close the producer: delegates to the kafka producer
        '''
        if self.producer is not None:
            self.producer.flush()   
            self.producer.close()        
        