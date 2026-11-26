"""
Kafka consumer of IasValues from the core topic.

Each received IasValue is sent to the listener (an IasValueListener subclass).

Delegates all Kafka consumption to an internal IasLogConsumer, wrapping the
raw log callback to decode JSON into IasValue objects.
"""
from datetime import datetime, timezone
from threading import Event
import logging

from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener


class IasValueListener:
    """
    Listener that receives IasValue objects from KafkaValueConsumer.

    Subclass this and override iasValueReceived to handle incoming values.
    """

    def iasValueReceived(self, iasValue: IasValue) -> None:
        """
        Called when a new IasValue is received from the BSDB topic.

        Must be overridden by subclasses.
        """
        raise NotImplementedError("Must override this method to get events")


class _ValueLogListener(IasLogListener):
    """
    Internal adapter that converts raw log strings into IasValue objects
    and forwards them to the user's IasValueListener.
    """

    def __init__(self, value_listener: IasValueListener, logger: logging.Logger):
        self._value_listener = value_listener
        self._logger = logger

    def iasLogReceived(self, log: str) -> None:
        try:
            iasValue = IasValue.fromJSon(log)
            iasValue.readFromBsdbTStamp = datetime.now(timezone.utc)
            iasValue.readFromBsdbTStampStr = Iso8601TStamp.datetimeToIsoTimestamp(
                iasValue.readFromBsdbTStamp
            )
            self._value_listener.iasValueReceived(iasValue)
        except Exception:
            self._logger.exception("Exception parsing IasValue from log [%s]", log)


class KafkaValueConsumer:
    '''
    Kafka consumer of IasValues.

    Each received IasValue is sent to the listener.

    The listener must inherit from IasValueListener.

    KafkaValueConsumer does not create a topic that must be created by the producer.
    If the topic does not exist, the thread waits until the producer creates it.

    KafkaValueConsumer wraps an IasLogConsumer that handles the actual Kafka polling.
    A boolean watchdog is set to True at every poll iteration.
    An Event passed to start() signals when the consumer is assigned to a partition.
    '''

    def __init__(self,
                 listener: IasValueListener,
                 kafkabrokers: str,
                 clientid: str,
                 groupid: str,
                 poll_timeout: float = 0.5):
        '''
        Constructor

        Params:
            listener the listener to send IasValues to
            kafkabrokers: Kafka brokers
            clientid: Kafka client ID
            groupid: Kafka group ID
            poll_timeout: the timeout (>0) for the Consumer.poll() function (seconds)
        '''
        if listener is None:
            raise ValueError("The listener can't be None")

        if not isinstance(listener, IasValueListener):
            raise ValueError("The listener must be a subclass of IasValueListener")

        if not kafkabrokers:
            raise ValueError("The kafka brokers can't be None or empty")

        self._logger = logging.getLogger(self.__class__.__name__)
        self._listener = listener

        from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

        log_listener = _ValueLogListener(listener, self._logger)
        self._consumer = IasLogConsumer(
            log_listener,
            kafkabrokers,
            IasKafkaHelper.topics['core'],
            clientid,
            groupid,
            poll_timeout,
        )

    def isSubscribed(self) -> bool:
        """
        Returns:
            True if the consumer is subscribed to a partition,
            False otherwise
        """
        return self._consumer.isSubscribed()

    def start(self, ready_event: Event = None):
        """
        Start polling events from the Kafka topic.

        Args:
            ready_event: If not None, the event is set when the consumer is
                subscribed to a partition and is getting events.
        """
        self._logger.info('Starting thread to poll for IasValues')
        self._consumer.start(ready_event=ready_event)

    def close(self):
        """
        Shuts down the consumer thread and closes the underlying Kafka consumer.
        """
        self._logger.debug("Closing...")
        self._consumer.close()
        self._logger.info("Closed")

    def getWatchdog(self) -> bool:
        """
        Return and reset the watch dog.

        Returns:
            True if the watchdog has been set, False otherwise
        """
        return self._consumer.getWatchdog()
