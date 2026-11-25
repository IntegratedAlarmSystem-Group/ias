"""
Unit tests for the KafkaValueConsumer delegation to IasLogConsumer.

Tests that KafkaValueConsumer correctly wraps IasLogConsumer and passes
IasValue objects to the listener.
"""
from threading import Event
from unittest.mock import MagicMock, patch

import pytest

from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer, IasValueListener, _ValueLogListener
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer


class DummyValueListener(IasValueListener):
    """Minimal listener for IasValue objects."""
    def __init__(self):
        self.received: list = []

    def iasValueReceived(self, ias_value):
        self.received.append(ias_value)


TEST_JSON = (
    '{"value":"0","pluginProductionTStamp":"1970-01-01T00:00:00.1",'
    '"sentToConverterTStamp":"1970-01-01T00:00:00.2",'
    '"receivedFromPluginTStamp":"1970-01-01T00:00:00.3",'
    '"convertedProductionTStamp":"1970-01-01T00:00:00.4",'
    '"sentToBsdbTStamp":"1970-01-01T00:00:00.5",'
    '"readFromBsdbTStamp":"1970-01-01T00:00:00.6",'
    '"productionTStamp":"1970-01-01T00:00:00.7",'
    '"depsFullRunningIds":[],'
    '"mode":"DEGRADED","iasValidity":"RELIABLE",'
    '"fullRunningId":"(Sys:S)@(Pl:P)@(C:C)@(V:IASIO)",'
    '"valueType":"LONG"}'
)


class TestValueLogListener:

    def test_decodes_json_and_forwards_to_value_listener(self):
        """iasLogReceived should parse JSON and forward IasValue to the user listener."""
        listener = DummyValueListener()
        log_listener = _ValueLogListener(listener, MagicMock())
        log_listener.iasLogReceived(TEST_JSON)

        assert len(listener.received) == 1
        ias_value = listener.received[0]
        assert ias_value.value == "0"
        assert ias_value.readFromBsdbTStamp is not None
        assert ias_value.readFromBsdbTStampStr is not None

    def test_logs_exception_on_bad_json(self):
        """iasLogReceived should catch and log exceptions on malformed JSON."""
        listener = DummyValueListener()
        mock_logger = MagicMock()
        log_listener = _ValueLogListener(listener, mock_logger)
        log_listener.iasLogReceived("not valid json{{{")

        assert len(listener.received) == 0
        mock_logger.exception.assert_called_once()


class TestKafkaValueConsumer:

    def _build_consumer(self, mock_log_consumer_instance):
        """Build a KafkaValueConsumer with a mocked IasLogConsumer."""
        mock_helper = MagicMock()
        mock_helper.topics = {'core': 'core'}
        with patch('IasKafkaUtils.KafkaValueConsumer.IasLogConsumer',
                   return_value=mock_log_consumer_instance):
            with patch.dict('sys.modules',
                            {'IasKafkaUtils.IaskafkaHelper': mock_helper}):
                consumer = KafkaValueConsumer(
                    listener=DummyValueListener(),
                    kafkabrokers='localhost:9092',
                    clientid='test-client',
                    groupid='test-group',
                )
        return consumer

    def test_creates_ias_log_consumer(self):
        """KafkaValueConsumer should instantiate an IasLogConsumer internally."""
        mock_consumer = MagicMock(spec=IasLogConsumer)
        consumer = self._build_consumer(mock_consumer)
        assert consumer._consumer is mock_consumer

    def test_is_subscribed_delegates(self):
        """isSubscribed should delegate to the internal IasLogConsumer."""
        mock_consumer = MagicMock()
        mock_consumer.isSubscribed.return_value = True
        consumer = self._build_consumer(mock_consumer)
        assert consumer.isSubscribed() is True
        mock_consumer.isSubscribed.assert_called_once()

    def test_is_getting_values_delegates(self):
        """isGettingValues should delegate to the internal IasLogConsumer."""
        mock_consumer = MagicMock()
        mock_consumer.isGettingLogs.return_value = False
        consumer = self._build_consumer(mock_consumer)
        assert consumer.isGettingValues() is False
        mock_consumer.isGettingLogs.assert_called_once()

    def test_start_delegates(self):
        """start() should delegate to the internal IasLogConsumer."""
        mock_consumer = MagicMock()
        consumer = self._build_consumer(mock_consumer)
        ready = Event()
        consumer.start(ready_event=ready)
        mock_consumer.start.assert_called_once_with(ready_event=ready)

    def test_start_without_ready_event(self):
        """start() without ready_event should delegate correctly."""
        mock_consumer = MagicMock()
        consumer = self._build_consumer(mock_consumer)
        consumer.start()
        mock_consumer.start.assert_called_once_with(ready_event=None)

    def test_close_delegates(self):
        """close() should delegate to the internal IasLogConsumer."""
        mock_consumer = MagicMock()
        consumer = self._build_consumer(mock_consumer)
        consumer.close()
        mock_consumer.close.assert_called_once()

    def test_get_watchdog_delegates(self):
        """getWatchdog() should delegate to the internal IasLogConsumer."""
        mock_consumer = MagicMock()
        mock_consumer.getWatchdog.return_value = True
        consumer = self._build_consumer(mock_consumer)
        assert consumer.getWatchdog() is True
        mock_consumer.getWatchdog.assert_called_once()

    def test_constructor_raises_on_none_listener(self):
        """__init__ should raise ValueError when listener is None."""
        with pytest.raises(ValueError, match="listener"):
            KafkaValueConsumer(
                listener=None,
                kafkabrokers='localhost:9092',
                clientid='test-client',
                groupid='test-group',
            )

    def test_constructor_raises_on_invalid_listener(self):
        """__init__ should raise ValueError when listener is not IasValueListener."""
        with pytest.raises(ValueError, match="listener"):
            KafkaValueConsumer(
                listener=object(),
                kafkabrokers='localhost:9092',
                clientid='test-client',
                groupid='test-group',
            )

    def test_constructor_raises_on_empty_brokers(self):
        """__init__ should raise ValueError when brokers are empty."""
        mock_helper = MagicMock()
        mock_helper.topics = {'core': 'core'}
        with patch.dict('sys.modules',
                        {'IasKafkaUtils.IaskafkaHelper': mock_helper}):
            with pytest.raises(ValueError, match="brokers"):
                KafkaValueConsumer(
                    listener=DummyValueListener(),
                    kafkabrokers='',
                    clientid='test-client',
                    groupid='test-group',
                )
