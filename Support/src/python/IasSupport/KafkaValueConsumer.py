'''
Created on Jun 12, 2018

@author: acaproni
'''
from kafka import KafkaConsumer
from threading import Thread
from IasSupport.IasValue import IasValue

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
    '''
    
    # The listener to send IasValues to
    listener = None
    
    # The kafka consumer
    consumer = None
    
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
        
        self.consumer = KafkaConsumer(
            topic,
            bootstrap_servers=kafkabrokers,
            client_id=clientid,
            group_id=groupid,
            auto_offset_reset="latest",
            consumer_timeout_ms=1000,
            enable_auto_commit=True)
        Thread.setDaemon(self, True)
        
    def run(self):
        """
        The thread to get IasValues from the BSDB
        """
        while True:
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
        