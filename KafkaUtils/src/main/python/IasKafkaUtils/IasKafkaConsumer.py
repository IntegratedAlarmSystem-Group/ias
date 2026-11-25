"""
Consume and send to a listener, the kafka events
published in a topic.
"""
import time
import logging
from threading import Thread, Lock, RLock, Event, current_thread
from confluent_kafka import Consumer, KafkaError
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
    """
    The consumer of IAS logs.

    Each received log is sent to the listener.

    To start getting logs, the start() function must be executed. If
    waitAssigmentTimeout > 0, start() polls synchronously on the calling
    thread until the consumer is assigned to a partition (or the timeout
    expires), then starts the background poll thread. If timeout is 0, the
    thread starts immediately and assignment happens asynchronously.

    IasLogConsumer implements a boolean watchdog that is set to True
    at every iteration of the thread (i.e. when data arrives or the timeout elapses).
    """

    def __init__(self,
                 listener: IasLogListener,
                 kafkabrokers: str,
                 topic: str,
                 clientid: str,
                 groupid: str,
                 poll_timeout = 0.5):
        '''
        Constructor
        
        Params:
            listener the listener to send logs to
            kafkabrokers: Kafka brokers
            topic: the kafka topic to get logs from
            clientid: Kafka client ID
            groupid: Kafka group ID
            poll_timeout: the timeout (>0) for the Consumer.poll() function (seconds)
        '''
        super().__init__()
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
        self._clientid = clientid

        if not groupid:
            raise ValueError("Invalid kafka group ID")
        self._groupid = groupid

        if poll_timeout <= 0:
            raise ValueError("Invalid poll timeout")
        self._poll_timeout = poll_timeout

        self.name = f"{IasLogConsumer.__name__}-{self._clientid}-{self._groupid}"

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
        self._terminate_thread: Event = Event()

        # Flags to not close consumer more than once
        self._closed: bool =  False

        # The lock for the consumer
        self._consumer_lock: RLock = RLock()

    def onAssign(self, consumer, partitions):
        for p in partitions:
            self._logger.info("Kafka consumer with client id %s and group id %s assigned to partition %d of topic %s",
                            self._clientid,
                            self._groupid,
                            p.partition,
                            repr(p.topic))

    def onLost(self, consumer, partitions):
        if self._closed:
            return
        for p in partitions:
            self._logger.warning("Kafka consumer with client id %s and group id %s lost partition %d of topic %s", 
                            self._clientid,
                            self._groupid,
                            p.partition,
                            repr(p.topic))

    def onError(self, kafka_error):
        self._logger.error("Kafka consumer with client id %s and group id %s got an error: %s", 
                           self._clientid,
                           self._groupid,
                           kafka_error.str())

    def onRevoke(self, consumer, partitions):
        if self._closed:
            return
        for p in partitions:
            self._logger.warning("Kafka consumer with client id %s and group id %s revoked partition %d of topic %s", 
                                  self._clientid,
                                  self._groupid,
                                  p.partition,
                                  repr(p.topic))

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to at least one partition, 
            False otherwise
        """
        if self._closed:
            return False
        with self._consumer_lock:
            return len(self._consumer.assignment())>0

    def isGettingLogs(self):
        """
        Returns:
            True if the consumer is getting events from the kafka topic partitions,
            False otherwise
        """
        if self._closed:
            return False
        return self.is_alive() and self.isSubscribed()

    def run(self):
        self._logger.info('Thread to poll logs started')
        try:
            while not self._terminate_thread.is_set():
                with self._consumer_lock:
                    msg = self._consumer.poll(timeout=self._poll_timeout)
                # Reset the watch dog
                with self._watchdog_lock:
                    self._watchdog = True
                if not msg:
                    # Timeout: no message received
                    continue
                if not len(self._consumer.assignment()):
                    self._logger.debug("Consumer for client id %s and group id %s NOT subscribed yet to topic %s",
                                       self._clientid,
                                       self._groupid,
                                       self._topic)
                    continue

                if msg.error() is not None:
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        # End of partition event
                        self._logger.error('Topic %s [partition %d] reached end at offset %d', msg.topic(), msg.partition(),
                                     msg.offset())
                    else:
                        self._logger.error('Error polling event %s', msg.error().name())
                    continue
                else:
                    try:
                        log = msg.value().decode("utf-8")
                    except Exception as e:
                        self._logger.exception("Error decoding log %s", str(msg.value()))
                        continue
                    try:
                        if not self._terminate_thread.is_set():
                            self._listener.iasLogReceived(log)
                    except Exception as e:
                        self._logger.exception("Exception caught from the listener of logs")
                        continue
        except Exception:
            traceback.print_exc()

        self._logger.info('Consumer thread for topic %s terminated for client id %s and group id %s', 
                          self._topic, self._clientid, self._groupid)

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
        with self._consumer_lock:
            self._logger.debug("Subscribing to topic %s", self._topic)
            self._consumer.subscribe([self._topic], 
                                     on_assign=self.onAssign, 
                                     on_lost=self.onLost, 
                                     on_revoke=self.onRevoke)

        if waitAssigmentTimeout > 0:
            self._logger.debug("Polling for assignment of client %s with group %s to topic %s (timeout %fs)",
                               self._clientid, self._groupid, self._topic, waitAssigmentTimeout)
            assignment_start = time.time()
            while not self._consumer.assignment() and (time.time() - assignment_start) < waitAssigmentTimeout:
                with self._consumer_lock:
                    self._consumer.poll(timeout=1.0)
            if not self._consumer.assignment():
                self._logger.warning("Client %s with group %s NOT assigned to topic %s after %f seconds",
                                     self._clientid, self._groupid, self._topic, waitAssigmentTimeout)
                return False
            self._logger.debug("Client %s with group %s assigned to topic %s",
                               self._clientid, self._groupid, self._topic)

        self._logger.info('Starting thread to poll events from topic %s', self._topic)
        Thread.start(self)

        self._logger.info('Consumer for client ID %s and group ID %s started', self._clientid, self._groupid)
        return self.isGettingLogs()

    def close(self):
        '''
        Shuts down the thread and close the consumer
        '''
        if self._closed:
            self._logger.warning("Already closed")
            return
        self._closed = True
        
        if self.is_alive() and current_thread() is not self:
            self._terminate_thread.set()
            self.join(5)  # Ensure the thread exited before closing the consumer
            if self.is_alive():
                self._logger.warning("The thread did not terminate in time")
        else:
            self._terminate_thread.set()

        # Close down consumer to commit final offsets.
        with self._consumer_lock:
            self._consumer.close()

        if not self.is_alive():
            # The thread never started
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
