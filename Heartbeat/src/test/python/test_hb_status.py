"""
Test the HB Status
"""
import pytest

from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus

class TestHbStatus():
    def testFromString(self):
        assert IasHeartbeatStatus.fromString("PARTIALLY_RUNNING") == IasHeartbeatStatus.PARTIALLY_RUNNING
        with pytest.raises(NotImplementedError) as nie:
            IasHeartbeatStatus.fromString("UnknownHbStatus")
