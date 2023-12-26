'''
Created on Jun 14, 2018

@author: acaproni
'''
from confluent_kafka import Producer
from IASLogging.logConf import Log

logger = logger=Log.getLogger(__file__)

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
                    kafkabrokers, topic, clientid)

        conf = { 'bootstrap.servers': kafkabrokers, 'client.id': clientid}
        
        self.producer = Producer(conf)
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
        self.producer.produce(self.topic, value=iasValueStr, key=id)
        self.producer.flush()
        
    def flush(self):
        '''
        Flush: delegates to the kafka producer
        '''
        self.producer.flush()
        
    def close(self):
        '''
        Close the producer: delegates to the kafka producer
        '''
        #self.producer.close()
        
        