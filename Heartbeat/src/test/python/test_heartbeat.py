"""
Test the Heartbeat
"""
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType

class TestHearbeat():
    def test_heartbeat_constr(self):
        stringRepr = "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        hbType = HeartbeatProducerType.SUPERVISOR
        name = "SupervisorWithKafka"
        hostname = "ias-fc40"
        hb = Heartbeat(hbType=hbType, name=name, hostName=hostname)

        assert hb.stringRepr == stringRepr
        assert hb.id == "SupervisorWithKafka:SUPERVISOR"

    def test_heartbeat_from_str(self):
        stringRepr = "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        hb = Heartbeat.fromStringRepr(stringRepr)
        assert hb.stringRepr == stringRepr
        assert hb.name == "SupervisorWithKafka"
        assert hb.hostname == "ias-fc40"
        assert hb.hbType == HeartbeatProducerType.SUPERVISOR
