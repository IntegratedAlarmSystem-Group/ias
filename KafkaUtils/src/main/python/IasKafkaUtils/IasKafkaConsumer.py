'''
Consume and send to a listener, the kafka events
published in a topic.
'''
import time
from threading import Thread, Lock
from confluent_kafka import Consumer, KafkaError
from confluent_kafka import TopicPartition
from IASLogging.logConf import Log
import traceback

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
    # The logger
    logger = Log.getLogger(__file__)

    # The listener to send logs to
    listener: IasLogListener = None

    # The kafka consumer
    consumer: Consumer = None

    # The name of the topic
    topic: str = None

    # Signal the thread to terminate
    terminateThread: bool = False

    # Signal if the thread is getting events from the topic 
    # This is not the same of starting the thread because
    # if the topic does not exist, the thread wait until
    # it is created but is not yet getting events
    isGettingEvents = False

    def __init__(self,
                 listener: IasLogListener,
                 kafkabrokers: str,
                 topic: str,
                 clientid: str,
                 groupid: str):
        '''
        Constructor
        
        @param listener the listener to send logs to
        '''
        Thread.__init__(self)
        if listener is None:
            raise ValueError("The listener can't be None")

        if not isinstance(listener, IasLogListener):
            raise ValueError("The listener must be a subclass of IasLogListener")
        self.listener = listener

        if topic is None:
            raise ValueError("The topic can't be None")
        self.topic = topic

        conf = {'bootstrap.servers': kafkabrokers,
                'client.id': clientid,
                'group.id': groupid,
                'enable.auto.commit': 'true',
                'allow.auto.create.topics': 'true',
                'auto.offset.reset': 'latest'}

        self.consumer = Consumer(conf, logger=self.logger)

        Thread.daemon = True

        # the watch dog 
        self.watchDog = False

        # The lock for the watch dog
        self.watchDogLock = Lock()

        # Confluent consumer does not create a topic even if the auto.create.topic=true
        # subscribed is set by onAssign and unlock the polling thread
        self.subscribed = False

        self.logger.info('Kafka consumer %s will connect to %s and topic %s', clientid, kafkabrokers, topic)

    def onAssign(self, consumer, partition):
        self.logger.info("Kafka consumer assigned to partition %s", partition)
        self.subscribed = True

    def onLost(self):
        self.logger.info("Partition lost")
        self.subscribed = False

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to a partition, 
            False otherwise
        """
        return self.subscribed

    def run(self):
        self.logger.info('Thread to poll logs started')
        try:

            self.isGettingEvents = True
            while not self.terminateThread:
                msg = self.consumer.poll(timeout=1.0)
                # Reset the watch dog
                with self.watchDogLock:
                    self.watchDog = True
                if msg is None or not self.subscribed:
                    self.logger.debug(f"Polling thread is subscribed { self.subscribed}")
                    continue

                if msg.error() is not None:
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        # End of partition event
                        self.logger.error('topic %s [partition %d] reached end at offset %d', msg.topic(), msg.partition(),
                                     msg.offset())
                    else:
                        self.logger.error('Error polling event %s', msg.error().str())
                        raise Exception(msg.error().str())
                else:
                    log = msg.value().decode("utf-8")
                    self.listener.iasLogReceived(log)
        except Exception:
            traceback.print_exc()
        finally:
            # Close down consumer to commit final offsets.
            self.consumer.close()
            self.isGettingEvents = False
        self.logger.info('Thread terminated')

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
        self.consumer.subscribe([self.topic], on_assign=self.onAssign)
        self.logger.info('Starting thread to poll events from topic %s', self.topic)
        self.terminateThread = False
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
        Shuts down the thread
        '''
        if not self.terminateThread:
            self.terminateThread = True
            self.join(5)  # Ensure the thread exited before closing the consumer
            self.consumer.close()
        else:
            self.logger.warn("Consumer aready terminated")

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
