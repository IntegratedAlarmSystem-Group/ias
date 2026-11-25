"""
Unit tests for the IasLogConsumer start() function.

Mocks the confluent_kafka Consumer to avoid needing a real Kafka broker.

Key behaviour under test:
- When ``waitAssigmentTimeout > 0``, the consumer polls on the calling
  thread.  On success the background thread starts.  On timeout the
  consumer is marked as **failed** and the thread is **never** started.
- When ``waitAssigmentTimeout == 0``, the thread starts immediately and
  assignment happens asynchronously via the ``ready_event`` callback.
- A failed consumer cannot be restarted.
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


# ---------------------------------------------------------------------------
# Mock builders
# ---------------------------------------------------------------------------

def _mock_consumer_assigns_on_first_poll():
    """
    Mock that fires the ``on_assign`` callback and reports a non-empty
    assignment on the first ``poll()`` call.
    """
    mock_consumer = MagicMock()
    mock_partition = MagicMock()
    mock_partition.partition = 0
    mock_partition.topic = 'TestTopic'

    on_assign_cb = [None]

    def capture_subscribe(topics, on_assign=None, on_lost=None, on_revoke=None):
        on_assign_cb[0] = on_assign

    poll_count = [0]

    def fake_poll(*args, **kwargs):
        poll_count[0] += 1
        # Fire on_assign on the very first poll
        if poll_count[0] == 1 and on_assign_cb[0] is not None:
            on_assign_cb[0](mock_consumer, [mock_partition])
        return None

    mock_consumer.subscribe = capture_subscribe
    mock_consumer.poll.side_effect = fake_poll
    mock_consumer.assignment.return_value = [mock_partition]
    return mock_consumer


def _mock_consumer_never_assigns():
    """Mock that never triggers ``on_assign`` — assignment always empty."""
    mock_consumer = MagicMock()

    def capture_subscribe(topics, on_assign=None, on_lost=None, on_revoke=None):
        pass

    mock_consumer.subscribe = capture_subscribe
    mock_consumer.poll.return_value = None
    mock_consumer.assignment.return_value = []
    return mock_consumer


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


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestIasLogConsumerStart():

    # -- Blocking assignment (timeout > 0) --

    def test_blocking_assignment_succeeds(self):
        """start() with timeout should return True and start the thread when assignment succeeds."""
        mock_consumer = _mock_consumer_assigns_on_first_poll()

        consumer = _build_consumer(mock_consumer)
        result = consumer.start(waitAssigmentTimeout=5)

        assert result is True
        assert not consumer.failed
        assert consumer.is_alive()

        consumer.close()

    def test_blocking_assignment_succeeds_sets_ready_event(self):
        """start() with timeout and ready_event should be set on success."""
        mock_consumer = _mock_consumer_assigns_on_first_poll()

        consumer = _build_consumer(mock_consumer)
        ready_event = Event()
        result = consumer.start(waitAssigmentTimeout=5, ready_event=ready_event)

        assert result is True
        assert ready_event.is_set()

        consumer.close()

    def test_blocking_assignment_timeout_marks_failed(self):
        """start() with timeout should return False and mark the consumer as failed when assignment never arrives."""
        mock_consumer = _mock_consumer_never_assigns()

        consumer = _build_consumer(mock_consumer)

        start_time = time.time()
        result = consumer.start(waitAssigmentTimeout=0.3)
        elapsed = time.time() - start_time

        assert result is False
        assert consumer.failed
        assert not consumer.is_alive(), "Thread must NOT be started on timeout"
        assert elapsed >= 0.2
        assert elapsed < 2.0

    def test_failed_consumer_cannot_be_restarted(self):
        """Once an IasLogConsumer fails, a second start() must return False."""
        mock_consumer = _mock_consumer_never_assigns()

        consumer = _build_consumer(mock_consumer)
        consumer.start(waitAssigmentTimeout=0.3)  # fails

        assert consumer.failed
        result2 = consumer.start(waitAssigmentTimeout=5)
        assert result2 is False

    # -- Async assignment (timeout == 0) --

    def test_async_start_returns_quickly(self):
        """start() with timeout=0 should return quickly (no blocking poll)."""
        mock_consumer = _mock_consumer_never_assigns()

        consumer = _build_consumer(mock_consumer)

        start_time = time.time()
        result = consumer.start(waitAssigmentTimeout=0)
        elapsed = time.time() - start_time

        assert elapsed < 1.0
        assert result is True
        assert consumer.is_alive()

        consumer.close()

    def test_async_start_sets_ready_event(self):
        """ready_event passed with timeout=0 must become the internal event."""
        mock_consumer = _mock_consumer_assigns_on_first_poll()

        consumer = _build_consumer(mock_consumer)
        ready_event = Event()
        consumer.start(waitAssigmentTimeout=0, ready_event=ready_event)

        assert consumer._start_ready_event is ready_event
        consumer.close()

    # -- Failed consumer guards --

    def test_isSubscribed_returns_false_when_failed(self):
        """A failed consumer must report isSubscribed() == False."""
        mock_consumer = _mock_consumer_never_assigns()
        consumer = _build_consumer(mock_consumer)
        consumer.start(waitAssigmentTimeout=0.3)  # fails

        assert not consumer.isSubscribed()

    def test_isGettingLogs_returns_false_when_failed(self):
        """A failed consumer must report isGettingLogs() == False."""
        mock_consumer = _mock_consumer_never_assigns()
        consumer = _build_consumer(mock_consumer)
        consumer.start(waitAssigmentTimeout=0.3)  # fails

        assert not consumer.isGettingLogs()

    # -- Close of failed consumer --

    def test_close_failed_consumer_does_not_error(self):
        """close() on a failed consumer (no thread running) must not error."""
        mock_consumer = _mock_consumer_never_assigns()
        consumer = _build_consumer(mock_consumer)
        consumer.start(waitAssigmentTimeout=0.3)  # fails

        consumer.close()  # should not raise

    # -- ready_event and timeout together --

    def test_both_timeout_and_ready_event(self):
        """Both timeout and ready_event can be used at the same time."""
        mock_consumer = _mock_consumer_assigns_on_first_poll()

        consumer = _build_consumer(mock_consumer)
        ready_event = Event()
        result = consumer.start(waitAssigmentTimeout=5, ready_event=ready_event)

        assert result is True
        assert ready_event.is_set()

        consumer.close()
