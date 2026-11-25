"""
Unit tests for the IasLogConsumer start() function.

Mocks the confluent_kafka Consumer to avoid needing a real Kafka broker.
"""
import time
from threading import Event
from unittest.mock import MagicMock, patch

import pytest

from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener


class DummyListener(IasLogListener):
    """Minimal listener that satisfies the isinstance check."""
    def iasLogReceived(self, log: str) -> None:
        pass


def _setup_assignment_mock(mock_consumer):
    """Set up mock so the first poll() triggers the onAssign callback."""
    mock_partition = MagicMock()
    mock_partition.partition = 0
    mock_partition.topic = 'TestTopic'
    mock_partition.offset = 0

    call_count = [0]

    def fake_poll(*args, **kwargs):
        if call_count[0] == 0:
            call_count[0] += 1
            cb = getattr(mock_consumer, 'captured_on_assign', None)
            if cb is not None:
                cb(mock_consumer, [mock_partition])
        return None

    def capture_subscribe(topics, on_assign=None, on_lost=None, on_revoke=None):
        mock_consumer.captured_on_assign = on_assign

    mock_consumer.subscribe = capture_subscribe
    mock_consumer.poll.side_effect = fake_poll
    mock_consumer.assignment.return_value = [mock_partition]


def _build_consumer(mock_consumer):
    """Build an IasLogConsumer with a mocked confluent_kafka Consumer."""
    with patch('IasKafkaUtils.IasKafkaConsumer.Consumer', return_value=mock_consumer):
        consumer = IasLogConsumer(
            listener=DummyListener(),
            kafkabrokers='localhost:9092',
            topic='TestTopic',
            clientid='test-client',
            groupid='test-group')
    return consumer


class TestIasLogConsumerStart():

    def test_start_with_timeout_assignment_succeeds(self):
        """start() with timeout should return True when assignment succeeds."""
        mock_consumer = MagicMock()
        _setup_assignment_mock(mock_consumer)

        consumer = _build_consumer(mock_consumer)
        result = consumer.start(waitAssigmentTimeout=5)

        assert result is True

    def test_start_with_timeout_no_assignment_times_out(self):
        """start() with timeout should return False when assignment never arrives."""
        mock_consumer = MagicMock()

        def capture_subscribe(topics, on_assign=None, on_lost=None, on_revoke=None):
            mock_consumer.captured_on_assign = on_assign

        mock_consumer.subscribe = capture_subscribe
        mock_consumer.poll.return_value = None
        mock_consumer.assignment.return_value = []

        consumer = _build_consumer(mock_consumer)

        start_time = time.time()
        result = consumer.start(waitAssigmentTimeout=0.5)
        elapsed = time.time() - start_time

        assert result is False
        assert elapsed >= 0.4
        assert elapsed < 2.0

    def test_start_without_timeout_returns_immediately(self):
        """start() with timeout=0 should return quickly without blocking."""
        mock_consumer = MagicMock()

        def capture_subscribe(topics, on_assign=None, on_lost=None, on_revoke=None):
            mock_consumer.captured_on_assign = on_assign

        mock_consumer.subscribe = capture_subscribe
        mock_consumer.poll.return_value = None
        mock_consumer.assignment.return_value = []

        consumer = _build_consumer(mock_consumer)

        start_time = time.time()
        consumer.start(waitAssigmentTimeout=0)
        elapsed = time.time() - start_time

        assert elapsed < 1.0

    def test_start_sets_ready_event_on_assignment(self):
        """start() should set the ready_event when the consumer is assigned."""
        mock_consumer = MagicMock()
        _setup_assignment_mock(mock_consumer)

        consumer = _build_consumer(mock_consumer)
        ready_event = Event()
        result = consumer.start(waitAssigmentTimeout=5, ready_event=ready_event)

        assert ready_event.is_set()
        assert result is True

    def test_start_ready_event_is_same_object(self):
        """ready_event passed by caller should be the same event used internally."""
        mock_consumer = MagicMock()
        _setup_assignment_mock(mock_consumer)

        consumer = _build_consumer(mock_consumer)
        ready_event = Event()
        consumer.start(waitAssigmentTimeout=0, ready_event=ready_event)

        assert consumer._start_ready_event is ready_event

    def test_start_without_ready_event_creates_internal_event(self):
        """start() without ready_event should create its own internal Event."""
        mock_consumer = MagicMock()
        _setup_assignment_mock(mock_consumer)

        consumer = _build_consumer(mock_consumer)
        consumer.start(waitAssigmentTimeout=5)

        assert consumer._start_ready_event is not None
        assert isinstance(consumer._start_ready_event, Event)
        assert consumer._start_ready_event.is_set()

    def test_start_timeout_and_ready_event_together(self):
        """Both timeout and ready_event can be used at the same time."""
        mock_consumer = MagicMock()
        _setup_assignment_mock(mock_consumer)

        consumer = _build_consumer(mock_consumer)
        ready_event = Event()
        result = consumer.start(waitAssigmentTimeout=5, ready_event=ready_event)

        assert result is True
        assert ready_event.is_set()
