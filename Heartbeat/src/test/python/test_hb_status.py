"""
Test the HB Status
"""
import pytest

from IasHeartbeat.HeartbeatStatus import HeartbeatStatus

class TestHbStatus():
    def testFromString(self):
        assert HeartbeatStatus.fromString("PARTIALLY_RUNNING") == HeartbeatStatus.PARTIALLY_RUNNING
        with pytest.raises(NotImplementedError) as nie:
            HeartbeatStatus.fromString("UnknownHbStatus")
