"""
Test the Heartbeat
"""
from IasHeartbeat.IasHeartbeat import IasHeartbeat
from IasHeartbeat.IasHeartbeatProducerType import IasHeartbeatProducerType

class TestHearbeat():
    def test_heartbeat_constr(self):
        stringRepr = "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        hbType = IasHeartbeatProducerType.SUPERVISOR
        name = "SupervisorWithKafka"
        hostname = "ias-fc40"
        hb = IasHeartbeat(hbType=hbType, name=name, hostName=hostname)

        assert hb.stringRepr == stringRepr
        assert hb.id == "SupervisorWithKafka:SUPERVISOR"

    def test_heartbeat_from_str(self):
        stringRepr = "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        hb = IasHeartbeat.fromStringRepr(stringRepr)
        assert hb.stringRepr == stringRepr
        assert hb.name == "SupervisorWithKafka"
        assert hb.hostname == "ias-fc40"
        assert hb.hbType == IasHeartbeatProducerType.SUPERVISOR
