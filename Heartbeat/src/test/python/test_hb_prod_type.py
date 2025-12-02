"""
Test the HB Status
"""
import pytest

from IasHeartbeat.IasHeartbeatProducerType import IasHeartbeatProducerType

class TestHbStatus():
    def test_from_string(self):
        assert IasHeartbeatProducerType.fromString("CLIENT") == IasHeartbeatProducerType.CLIENT
        with pytest.raises(NotImplementedError) as nie:
            IasHeartbeatProducerType.fromString("UnknownHbStatus")
