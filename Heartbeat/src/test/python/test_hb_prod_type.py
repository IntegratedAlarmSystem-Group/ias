"""
Test the HB Status
"""
import pytest

from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType

class TestHbStatus():
    def test_from_string(self):
        assert HeartbeatProducerType.fromString("CLIENT") == HeartbeatProducerType.CLIENT
        with pytest.raises(NotImplementedError) as nie:
            HeartbeatProducerType.fromString("UnknownHbStatus")
