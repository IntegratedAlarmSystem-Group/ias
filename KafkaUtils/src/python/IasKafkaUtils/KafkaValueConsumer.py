'''
Created on Jun 12, 2018

@author: acaproni
'''
import logging, time
from kafka import KafkaConsumer
from threading import Thread
from IasBasicTypes.IasValue import IasValue
from kafka.structs import TopicPartition

logger = logging.getLogger(__file__)

class IasValueListener(object):
    """
    The class to get IasValue
    
    The listener passed to KafkaValueConsumer
    must be a subclass of IasValueListener
    """
    
    def iasValueReceived(self,iasValue):
        """
        The callback to notify new IasValues
        received from the BSDB
        
        The implementation must override this method
        """
        raise NotImplementedError("Must override this method to get events")
    

class KafkaValueConsumer(Thread):
    '''
    Kafka consumer of IasValues.
    
    Each received IasValue is sent to the listener.
    
    The listener must inherit from IasValueListener
    
    KafkaValueConsumer does not create a topic that must be created by the producer.
    If the topic does not exists, the thread waits antil the producer creates it.
    '''
    
    # The listener to send IasValues to
    listener = None
    
    # The kafka consumer
    consumer = None
    
    # The topic
    topic = None
    
    # Signal the thread to terminate
    terminateThread = False
    
    # Signal if the thread is getting event from the topic 
    # This is not teh same of starting the thread vecause
    ## if the topic does not exist, the thread wait until
    # it is created but is not yet getting events
    isGettingEvents = False
    
    def __init__(self, 
                 listener,
                 kafkabrokers,
                 topic,
                 clientid,
                 groupid):
        '''
        Constructor
        
        @param listener the listener to send IasValues to
        '''
        Thread.__init__(self)
        if listener is None:
            raise ValueError("The listener can't be None")
            
        if not isinstance(listener,IasValueListener):
            raise ValueError("The listener msut be a subclass of IasValueListener")
        self.listener=listener
        
        if topic is None:
            raise ValueError("The topic can't be None")
        self.topic=topic
        
        self.consumer = KafkaConsumer(
            bootstrap_servers=kafkabrokers,
            client_id=clientid,
            enable_auto_commit=True)
        Thread.setDaemon(self, True)
        logger.info('Kafka consumer %s connected to %s and topic %s',clientid,kafkabrokers,topic)
        
    def run(self):
        """
        The thread to get IasValues from the BSDB
        """
        logger.info('Thread to poll for IasValues started')
        
        partitionsIds=None
        n = 1 
        if not self.terminateThread:
            while partitionsIds is None: 
                partitionsIds=self.consumer.partitions_for_topic(self.topic)
                if partitionsIds is None:
                    if n%10==0:
                        logger.info("Waiting for topic %s to be created",self.topic)
                    n = n + 1
                    time.sleep(0.100)
            
            partitionsIds=self.consumer.partitions_for_topic(self.topic)
            logger.info('%d partitions found on topic %s: %s',len(partitionsIds),self.topic,partitionsIds)
            partitions=[]
            for pId in partitionsIds:
                partitions.append(TopicPartition(self.topic,pId))
            self.consumer.assign(partitions)
            
            self.consumer.seek_to_end()
        
        self.isGettingEvents=True
        while True and not self.terminateThread:
            try:
                messages = self.consumer.poll(500)
            except:
                KeyboardInterrupt
                break
            for listOfConsumerRecords in messages.values():
                for cr in listOfConsumerRecords:
                    json = cr.value.decode("utf-8")
                    iasValue = IasValue.fromJSon(json)
                    self.listener.iasValueReceived(iasValue)
        self.isGettingEvents=False
        logger.info('Thread terminated')
        
    def start(self):
        logger.info('Starting thread to poll for IasValues')
        Thread.start(self)
        
    def close(self):
        '''
        Shuts down the thread
        '''
        self.terminateThread = True
        