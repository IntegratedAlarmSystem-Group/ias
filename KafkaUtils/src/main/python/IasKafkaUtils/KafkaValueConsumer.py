'''
Created on Jun 12, 2018

@author: acaproni
'''
from datetime import datetime
from datetime import timezone
from threading import Thread, Lock
import traceback
import time
from confluent_kafka import Consumer, KafkaError

from IASLogging.logConf import Log
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

logger = Log.getLogger(__file__)


class IasValueListener(object):
    """
    The class to get IasValue
    
    The listener passed to KafkaValueConsumer
    must be a subclass of IasValueListener
    """

    def iasValueReceived(self, iasValue):
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
    
    The listener must inherit from IasValueListener.
    
    KafkaValueConsumer does not create a topic that must be created by the producer.
    If the topic does not exist, the thread waits until the producer creates it.

    KafkaValueConsumer implements a boolean watchdog that is set to True
    at every iteration of the thread (i.e. when data arrives or the timeout elapses)
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
    # This is not teh same of starting the thread because
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

        if not isinstance(listener, IasValueListener):
            raise ValueError("The listener must be a subclass of IasValueListener")
        self.listener = listener

        if topic is None:
            raise ValueError("The topic can't be None")
        self.topic = topic

        self.kafkaBrokers = kafkabrokers

        conf = {'bootstrap.servers': kafkabrokers,
                'client.id': clientid,
                'group.id': groupid,
                'enable.auto.commit': 'true',
                'allow.auto.create.topics': 'true',
                'auto.offset.reset': 'latest'}

        self.consumer = Consumer(conf, logger=logger)

        Thread.daemon = True

        # the watch dog 
        self.watchDog = False

        # The lock for the watch dog
        self.watchDogLock = Lock()

        # Confluent consumer does not create a topic even if the auto.create.topic=true
        # subscribed is set by onAssign and unlock the polling thread
        self.subscribed = False

        logger.info('Kafka consumer %s will connect to %s and topic %s', clientid, kafkabrokers, topic)

    def onAssign(self, consumer, partition):
        logger.info("Kafka consumer assigned to partition %s", partition)
        self.subscribed = True

    def onLost(self):
        logger.info("Partition lost")
        self.subscribed = False

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to a partition, 
            False otherwise
        """
        return self.subscribed

    def run(self):
        logger.info('Thread to poll for Kafka logs started')
        try:

            # For some reason the python client does not create the topic and this
            # function hangs forever waiting to subscribe
            # So we force a topic reation before subscribing
            if IasKafkaHelper.createTopic(self.topic, self.kafkaBrokers):
                logger.debug("Topic %s created", self.topic)
            else:
                logger.debug("Topic %s exists", self.topic)

            self.consumer.subscribe([self.topic], on_assign=self.onAssign)

            self.isGettingEvents = True
            while not self.terminateThread:
                msg = self.consumer.poll(timeout=1.0)
                # Reset the watch dog
                with self.watchDogLock:
                    self.watchDog = True
                if msg is None or not self.subscribed:
                    logger.debug(f"Polling thread is subscribed { self.subscribed}")
                    continue

                if msg.error() is not None:
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        # End of partition event
                        logger.error('topic %s [partition %d] reached end at offset %d', msg.topic(), msg.partition(),
                                     msg.offset())
                    else:
                        logger.error('Error polling event %s', msg.error().str())
                        raise Exception(msg.error().str())
                else:
                    json = msg.value().decode("utf-8")
                    iasValue = IasValue.fromJSon(json)
                    iasValue.readFromBsdbTStamp = datetime.now(timezone.utc)
                    iasValue.readFromBsdbTStampStr = Iso8601TStamp.datetimeToIsoTimestamp(iasValue.readFromBsdbTStamp)
                    self.listener.iasValueReceived(iasValue)
        except Exception:
            traceback.print_exc()
        finally:
            # Close down consumer to commit final offsets.
            self.consumer.close()
            self.isGettingEvents = False
        logger.info('Thread terminated')

    def start(self):
        logger.info('Starting thread to poll for IasValues')
        self.terminateThread = False
        Thread.start(self)

    def close(self):
        '''
        Shuts down the thread
        '''
        self.terminateThread = True
        self.join(5)  # Ensure the thread exited before closing the consumer
        self.consumer.close()

    def getWatchdog(self):
        """
        Return and reset the watch dog.

        Returns:
        bool: True if the watchdog has been set, False otherwise
        """
        with self.watchDogLock:
            ret = self.watchDog
            self.watchDog = False
        return ret
