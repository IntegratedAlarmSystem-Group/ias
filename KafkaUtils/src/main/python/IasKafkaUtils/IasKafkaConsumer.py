'''
Consume and send to a listener, the kafka events
published in a topic.
'''
import time
import logging
from threading import Thread, Lock, Event
from confluent_kafka import Consumer, KafkaError
from confluent_kafka import TopicPartition
import traceback

from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

class IasLogListener:
    """
    The listener of logs read from IAS topics
    """

    def iasLogReceived(self, log: str) -> None:
        """
        The callback to notify new IasValues
        received from the BSDB
        
        The implementation must override this method
        """
        raise NotImplementedError("Must override this method to get events")
    
class IasLogConsumer(Thread):
    '''
    The consumer of IAS logs.
    
    Each received log is sent to the listener.
    
    To start getting logs, the start() function must be executed.
    To effectively get logs, the consumer must be subscribed to the topic (@see #230).
    The assignment might happen some seconds after start() terminates.
    start() waits for the assigment (or timeout) is the optional timeout is provided; alternatively
    IsSubscribed() can be invoked to know if the consumer is assigned to the topic.

    IasLogConsumer implements a boolean watchdog that is set to True
    at every iteration of the thread (i.e. new when data arrives or the timeout elapses)

    '''

    def __init__(self,
                 listener: IasLogListener,
                 kafkabrokers: str,
                 topic: str,
                 clientid: str,
                 groupid: str):
        '''
        Constructor
        
        Params:
            listener the listener to send logs to
            kafkabrokers: Kafka brokers
            topic: the kafka topic to get logs from
            clientid: Kafka client ID
            groupid: Kafka group ID
        '''
        Thread.__init__(self)
        # The logger
        self._logger = logging.getLogger(IasLogConsumer.__name__)
        if not listener:
            raise ValueError("The listener can't be None")

        if not isinstance(listener, IasLogListener):
            raise ValueError("The listener must be a subclass of IasLogListener")

        # The listener of logs
        self._listener: IasLogListener = listener

        if not topic:
            raise ValueError("The topic can't be None")
        self._topic = topic

        if not kafkabrokers:
            raise ValueError("Invalid kafka brokers")
        self._kafka_brokers = kafkabrokers

        if not clientid:
            raise ValueError("Invalid kafka client ID")

        if not groupid:
                    raise ValueError("Invalid kafka group ID")

        conf = {'bootstrap.servers': kafkabrokers,
                'client.id': clientid,
                'group.id': groupid,
                'enable.auto.commit': 'true',
                'allow.auto.create.topics': 'true',
                'auto.offset.reset': 'latest',
                'error_cb': self.onError}

        # The kafka consumer
        self._consumer: Consumer = Consumer(conf, logger=self._logger)

        self.daemon = True

        # the watch dog 
        self._watchdog: bool = False

        # The lock for the watch dog
        self._watchdog_lock: Lock = Lock()

        self._logger.info('Kafka consumer %s will connect to %s and topic %s', clientid, kafkabrokers, topic)

        # Signal the thread to terminate
        terminateThread: Event = Event()

        # Flags to not close consumer more than once
        self._closed: bool =  False


    def onAssign(self, consumer, partition):
        self._logger.info("Kafka consumer assigned to partition %s", partition)

    def onLost(self, consumer, partition):
        self._logger.info("Partition lost %s", partition)

    def onError(self, kafka_error):
        self._logger.error("Kafka error: %s", kafka_error.str())

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to at least one partition, 
            False otherwise
        """
        return len(self._consumer.assignment())>0

    def isGettingLogs(self):
        """
        Returns:
            True if the consumer is getting events from the kafka topic partitions,
            False otherwise
        """
        return self.is_alive() and self.isSubscribed()

    def run(self):
        self._logger.info('Thread to poll logs started')
        try:
            while not self.terminateThread:
                msg = self._consumer.poll(timeout=1.0)
                # Reset the watch dog
                with self._watchdog_lock:
                    self._watchdog = True
                if not msg or not self.isSubscribed():
                    self._logger.debug(f"Polling thread is {'' if self.isSubscribed() else 'NOT '}subscribed to topic {self._topic}")
                    continue

                if msg.error() is not None:
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        # End of partition event
                        self._logger.error('topic %s [partition %d] reached end at offset %d', msg.topic(), msg.partition(),
                                     msg.offset())
                    else:
                        self._logger.error('Error polling event %s', msg.error().name())
                    continue
                else:
                    try:
                        log = msg.value().decode("utf-8")
                    except Exception as e:
                        self._logger.exception("Error decoding log %s", str(msg.value()), e)
                        continue
                    try:
                        self._listener.iasLogReceived(log)
                    except Exception as e:
                        self._logger.exception("Exception caught from the listener of logs", e)
                        continue
        except Exception:
            traceback.print_exc()
        # Close down consumer to commit final offsets.
        self._consumer.close()
        self._logger.info('Thread terminated')

    def start(self, waitAssigmentTimeout: float = 0) -> bool:
        """
        Start the consumer

        This function starts the cosumer thread to get logs from the kafka topic.

        If a timeout greater than 1 is provided, the functions waits for the assignemt to the topic
        before returning.

        Args: 
            waitAssignemnttimeout: the time to wait for the assignment (seconds)
        Returns:
            True if the consumer is assigned to the topic, False otherwise
        """
         # For some reason the python client does not create the topic and this
        # function hangs forever waiting to subscribe
        # So we force a topic creation before subscribing
        if IasKafkaHelper.createTopic(self._topic, self._kafka_brokers):
            self._logger.debug("Topic %s created", self._topic)
        else:
            self._logger.debug("Topic %s exists", self._topic)
        self._consumer.subscribe([self._topic], on_assign=self.onAssign)
        self._logger.info('Starting thread to poll events from topic %s', self._topic)
        Thread.start(self)

        if waitAssigmentTimeout>=1:
            # Wait for assignment
            poll_time = 0.250
            start_time = time.time()
            while not self.isSubscribed() and time.time()<start_time+waitAssigmentTimeout:
                time.sleep(poll_time)
            
        return self.isSubscribed()

    def close(self):
        '''
        Shuts down the thread and close the consumer
        '''
        if self._closed:
            self._logger.warning("Already closed")
            return
        
        if self.is_alive():
            self.terminateThread.set()
            self.join(5)  # Ensure the thread exited before closing the consumer
            if self.is_alive():
                self._logger.warning("The thread did not terminate in time")

        if not self.is_alive():
            # The thread never started
            self._consumer.close()
            self._logger.info("Consumer closed")

    def getWatchdog(self):
        """
        Return and reset the watch dog.

        Returns:
        bool: True if the watchdog has been set, False otherwise
        """
        with self._watchdog_lock:
            ret = self._watchdog
            self._watchdog = False
        return ret
