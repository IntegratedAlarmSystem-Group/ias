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

        # Event used only by start() to signal when the consumer is assigned to a partition
        self._start_ready_event: Event | None = None

        # Flags to not close consumer more than once
        self._closed: bool =  False

        # The lock for the consumer
        self._consumer_lock: RLock = RLock()

        # Flag set when start() with timeout fails. A failed consumer cannot
        # be restarted — the caller must instantiate a new object.
        self._failed: bool = False

        # Cached subscription state, updated by onAssign/onLost callbacks.
        # Allows lock-free reads of subscription status.
        self._assigned: bool = False

        # Lock only for the _assigned flag (separate from _consumer_lock).
        self._assigned_lock: Lock = Lock()

    def onAssign(self, consumer, partitions):
        with self._assigned_lock:
            self._assigned = len(consumer.assignment()) > 0
        for p in partitions:
            self._logger.info("Kafka consumer with client id %s and group id %s assigned to partition %d of topic %s",
                            self._clientid,
                            self._groupid,
                            p.partition,
                            repr(p.topic))
        if self._start_ready_event is not None:
            self._start_ready_event.set()

    def onLost(self, consumer, partitions):
        with self._assigned_lock:
            self._assigned = len(consumer.assignment()) > 0
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
        with self._assigned_lock:
            self._assigned = len(consumer.assignment()) > 0
        if self._closed:
            return
        for p in partitions:
            self._logger.warning("Kafka consumer with client id %s and group id %s revoked partition %d of topic %s", 
                                   self._clientid,
                                   self._groupid,
                                   p.partition,
                                   repr(p.topic))

    def _poll_for_assignment(self, timeout: float) -> bool:
        """
        Poll the consumer on the calling thread until it is assigned to at
        least one partition or the timeout expires.

        This method subscribes the consumer to its topic, then repeatedly
        calls ``poll()`` triggering the underlying librdkafka callbacks (e.g.
        ``on_assign``).  If assignment occurs within the timeout it returns
        ``True`` and the caller is free to start the background thread.
        If the timeout elapses the consumer is marked as **failed** — it can
        no longer be used and the caller must create a fresh instance.

        Args:
            timeout: maximum time to wait for partition assignment (seconds,
                must be > 0)

        Returns:
            ``True`` if the consumer was assigned to at least one partition,
            ``False`` on timeout (the consumer is marked as failed).
        """
        self._start_ready_event = Event()
        with self._consumer_lock:
            self._logger.debug("Subscribing to topic %s", self._topic)
            self._consumer.subscribe([self._topic],
                                     on_assign=self.onAssign,
                                     on_lost=self.onLost,
                                     on_revoke=self.onRevoke)

        end_time = time.time() + timeout
        with self._consumer_lock:
            while time.time() < end_time:
                remaining = end_time - time.time()
                if remaining <= 0:
                    break
                self._consumer.poll(timeout=min(remaining, 0.5))
                if len(self._consumer.assignment()) > 0:
                    self._logger.info(
                        "Client %s with group %s assigned to topic %s on calling thread",
                        self._clientid, self._groupid, self._topic)
                    return True

        # Timeout expired — mark the consumer as permanently failed.
        self._failed = True
        self._logger.error(
            "Client %s with group %s FAILED to connect to topic %s after %.*f seconds. "
            "This consumer instance is now INVALID and cannot be used. "
            "Instantiate a new IasLogConsumer to retry.",
            self._clientid, self._groupid, self._topic, 1, timeout)
        return False

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to at least one partition,
            False otherwise (including when the consumer has failed or is closed).

        Uses a cached flag updated by onAssign/onLost/onRevoke callbacks,
        avoiding the need to hold _consumer_lock.
        """
        if self._closed or self._failed:
            return False
        with self._assigned_lock:
            return self._assigned

    def isGettingLogs(self):
        """
        Returns:
            True if the consumer is getting events from the kafka topic partitions,
            False otherwise (including when the consumer has failed or is closed)
        """
        if self._closed or self._failed:
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

    def start(self, waitAssigmentTimeout: float = 0, ready_event: Event = None) -> bool:
        """
        Start the consumer

        This function starts the consumer thread to get logs from the kafka topic.

        **Behaviour with timeout** (``waitAssigmentTimeout > 0``):
        The method first polls on the **calling thread** until the consumer is
        assigned to a partition or the timeout expires.  If the connection
        succeeds, the background poll thread is spawned and ``True`` is
        returned.  If the timeout elapses the consumer is marked as **failed**
        (the underlying Kafka consumer is closed) and ``False`` is returned.
        A failed consumer **cannot** be restarted — the caller must create a
        new ``IasLogConsumer`` instance and try again.

        **Behaviour without timeout** (``waitAssigmentTimeout == 0``, the
        default): the background poll thread is started immediately.  The
        ``ready_event`` (if provided) is set when the consumer is assigned on
        the background thread.

        If ``ready_event`` is provided it is set when the consumer is
        subscribed to a partition, so the caller can wait asynchronously on
        it.

        Args:
            waitAssigmentTimeout: the time to wait for the assignment before
                starting the background thread (seconds, 0 = no wait, start
                asynchronously).
            ready_event: optional event also set when the consumer is
                assigned to a partition.

        Returns:
            ``True`` if the consumer is started and assigned to the topic,
            ``False`` otherwise.  When ``False`` is returned with a timeout,
            the consumer has been marked as failed and must be discarded.
        """
        if self._failed:
            self._logger.error(
                "Cannot start failed consumer %s (group %s). "
                "Instantiate a new IasLogConsumer to retry.",
                self._clientid, self._groupid)
            return False

        if waitAssigmentTimeout > 0:
            # Block on the calling thread until the connection succeeds or the
            # timeout expires.  _poll_for_assignment marks self._failed on
            # timeout and logs a clear error message.
            if not self._poll_for_assignment(waitAssigmentTimeout):
                return False

            # Assignment succeeded — start background thread.
            self._logger.info(
                'Starting thread to poll events from topic %s', self._topic)
            Thread.start(self)

            if ready_event is not None:
                ready_event.set()

            self._logger.info(
                'Consumer for client ID %s and group ID %s started (blocking)',
                self._clientid, self._groupid)
            return True

        # No timeout — start the thread and let assignment happen asynchronously.
        self._start_ready_event = ready_event if ready_event is not None else Event()
        with self._consumer_lock:
            self._logger.debug("Subscribing to topic %s", self._topic)
            self._consumer.subscribe([self._topic],
                                     on_assign=self.onAssign,
                                     on_lost=self.onLost,
                                     on_revoke=self.onRevoke)

        self._logger.info('Starting thread to poll events from topic %s', self._topic)
        Thread.start(self)

        self._logger.info('Consumer for client ID %s and group ID %s started (async)',
                          self._clientid, self._groupid)
        return True

    def close(self):
        '''
        Shuts down the thread and close the consumer.

        When the consumer has failed (``failed`` property is ``True``), the
        background thread was never started — this method simply closes the
        underlying Kafka consumer and logs that the failed consumer was cleaned
        up.
        '''
        if self._closed:
            self._logger.warning("Already closed")
            return
        self._closed = True
        with self._assigned_lock:
            self._assigned = False

        if self._failed:
            # The thread was never started; just close the underlying consumer
            # to release the network resources.
            self._logger.info(
                "Closing failed consumer for client %s (group %s) — no background thread to join",
                self._clientid, self._groupid)
            with self._consumer_lock:
                self._consumer.close()
            return

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

    @property
    def failed(self) -> bool:
        """
        Returns:
            True if the consumer connection has failed.  A failed consumer
            cannot be restarted — the caller must create a new instance.
        """
        return self._failed
