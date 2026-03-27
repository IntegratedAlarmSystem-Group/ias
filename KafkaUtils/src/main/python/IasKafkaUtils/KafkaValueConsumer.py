'''
Created on Jun 12, 2018

@author: acaproni
'''
from datetime import datetime
from datetime import timezone
from threading import Thread, Lock, Event
import traceback
import logging
from confluent_kafka import Consumer, KafkaError

from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

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

    The KafkaValueConsumer sets an Event, passed in the start() function,
    to signal when the thread is subscribed to a partition and is getting events.
    The same information can be get by calling isSubscribed().
    '''

    def __init__(self,
                 listener,
                 kafkabrokers: str,
                 topic: str,
                 clientid,
                 groupid):
        '''
        Constructor
        
        @param listener the listener to send IasValues to
        '''
        Thread.__init__(self)
        self._logger = logging.getLogger(self.__class__.__name__)

        if listener is None:
            raise ValueError("The listener can't be None")

        if not isinstance(listener, IasValueListener):
            raise ValueError("The listener must be a subclass of IasValueListener")
        
        # The listener to send IasValues to
        self.listener = listener

        if topic is None:
            raise ValueError("The topic can't be None")
        # The kafka topic
        self.topic: str = topic

        self.kafkaBrokers: str = kafkabrokers
        if not self.kafkaBrokers:
            raise ValueError("The kafka brokers can't be None or empty")

        conf = {'bootstrap.servers': kafkabrokers,
                'client.id': clientid,
                'group.id': groupid,
                'enable.auto.commit': 'true',
                'allow.auto.create.topics': 'true',
                'auto.offset.reset': 'latest',
                'error_cb': self.onError}

        # The kafka consumer
        self.consumer = Consumer(conf, logger=self._logger)

        # Signal if the thread is getting event from the topic 
        # This is not teh same of starting the thread because
        ## if the topic does not exist, the thread wait until
        # it is created but is not yet getting events
        self.isGettingEvents = False

          # Signal the thread to terminate
        self.terminateThread: Event = Event()

        Thread.daemon = True

        # the watch dog 
        self.watchDog = False

        # The lock for the watch dog
        self.watchDogLock = Lock()

        # Confluent consumer does not create a topic even if the auto.create.topic=true
        # subscribed is set by onAssign and unlock the polling thread
        self.subscribed = False

        # Singnal that the self.ready_event Event must be set, if not None
        self.ready_event_to_set_flag = False

        # The event passed in start() to signal when the thread is subscribed and getting events
        #
        # It will not be assigned in onAssign but after the next successfull poll
        self.ready_event: Event|None = None

        self._logger.info('Kafka consumer %s will connect to %s and topic %s', clientid, kafkabrokers, topic)

    # Note that this callback is executed when a new partition is assigned but the consumer
    # is not yet subscribed to the topic (it will be after the next successfull poll)
    # It means that items pushed immediately after onAssign is executed before the subscription 
    # takes place may not be received until the next poll 
    # (this is also true because the consumer property auto.offset.reset is set to latest, u
    # sing earliest would change this behaviour)
    def onAssign(self, consumer, partition):
        self._logger.info("Kafka consumer assigned to partition %s", partition)
        if self.ready_event is not None:
            self.ready_event_to_set_flag = True
        self.subscribed = True


    def onLost(self, consumer, partitions):
        self._logger.warning("Kafka consumer lost partitions %s", partitions)
        self.subscribed = False
        if self.ready_event is not None:
            self.ready_event.clear()
        self.ready_event_to_set_flag = False

    def onError(self, kafka_error):
        self._logger.error("Kafka error: %s", kafka_error.str())

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to a partition, 
            False otherwise
        """
        return self.subscribed

    def run(self):
        self._logger.info('Thread to poll for Kafka logs started')
        # Create the topic before subscribing
        if IasKafkaHelper.createTopic(self.topic, self.kafkaBrokers):
            self._logger.debug("Topic %s created", self.topic)
        else:
            self._logger.debug("Topic already %s exists", self.topic)

        self.consumer.subscribe([self.topic], on_assign=self.onAssign, on_revoke=self.onLost)
        self._logger.debug("Subscribed to topic %s", self.topic)

        self.isGettingEvents = True
        while not self.terminateThread.is_set():
            msg = self.consumer.poll(timeout=1.0)
            # Reset the watch dog
            with self.watchDogLock:
                self.watchDog = True

            if not self.subscribed:
                continue
            elif self.ready_event_to_set_flag and self.ready_event is not None:
                # Set the event that the consumer is ready after the first successful poll
                # after the subscription
                self.ready_event.set()
                self.ready_event_to_set_flag = False
            
            if msg is None: # No message received within the timeout
                self._logger.debug(f"Polling thread is NOT subscribed { self.subscribed}")
                continue

            if msg.error() is not None:
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    # End of partition event
                    self._logger.error('topic %s [partition %d] reached end at offset %d', msg.topic(), msg.partition(),
                                    msg.offset())
                else:
                    self._logger.error('Error polling event %s', msg.error().str())
                continue
            else:
                json = ""
                try:
                    json = msg.value().decode("utf-8")
                    iasValue = IasValue.fromJSon(json)
                    iasValue.readFromBsdbTStamp = datetime.now(timezone.utc)
                    iasValue.readFromBsdbTStampStr = Iso8601TStamp.datetimeToIsoTimestamp(iasValue.readFromBsdbTStamp)
                except Exception as e:
                    self._logger.error("Exception parsing a log [%s]", json, e)
                    continue
                try:
                    self.listener.iasValueReceived(iasValue)
                except Exception as e:
                    self._logger.error("Exception caught from the log listener [%s]", json, e)
        # Close the consumer to commit final offsets.
        self.consumer.close()
        self.isGettingEvents = False
        self._logger.info('Thread terminated')

    def start(self, ready_event: Event = None):
        """
        Start polling events from the core topic.
        
        :param ready_event: If not None, the event is set when the thread is subscribed to a partition and is getting events.
        :type ready_event: Event
        """
        self._logger.info('Starting thread to poll for IasValues')
        self.ready_event = ready_event
        Thread.start(self)

    def close(self):
        '''
        Shuts down the thread
        '''
        self._logger.debug("Closing...")
        self.terminateThread.set()
        self.join(5)  # Ensure the thread exited before closing the consumer
        self.consumer.close()
        self._logger.info("Closed")

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
