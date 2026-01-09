from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus

class HbMessageTest():
    """
    Test the HeartbeatMessage serialization/deserialization
    """
    
    def test_from_str_no_props(self):
        hbMsgStr = '{"timestamp":"2025-01-03T09:58:56.641","hbStringrepresentation":"SupervisorWithKafka:SUPERVISOR@ias-fc40","state":"RUNNING"}'
        hbMsg = HeartbeatMessage.fromJSON(hbMsgStr)

        assert hbMsg.timestamp == "2025-01-03T09:58:56.641"
        assert hbMsg.hbStringrepresentation == "SupervisorWithKafka:SUPERVISOR@ias-fc40"
        assert hbMsg.state == IasHeartbeatStatus.RUNNING
        assert hbMsg.props is None

    def test_from_str_with_props(self):
        hbMsgStr = '{"timestamp":"2025-01-03T11:05:57.361","hbStringrepresentation":"SupervisorID:SUPERVISOR@ias-fc40","state":"RUNNING","props":{"key1":"prop1","key2":"prop2"}}'
        hbMsg = HeartbeatMessage.fromJSON(hbMsgStr)

        assert hbMsg.timestamp == "2025-01-03T11:05:57.361"
        assert hbMsg.hbStringrepresentation == "SupervisorID:SUPERVISOR@ias-fc40"
        assert hbMsg.state == IasHeartbeatStatus.RUNNING
        assert hbMsg.props == {"key1":"prop1","key2":"prop2"}

    def test_from_hb_no_props(self):
        hbMsgStr = '{"timestamp":"2025-01-03T09:58:56.641","hbStringrepresentation":"SupervisorWithKafka:SUPERVISOR@ias-fc40","state":"PARTIALLY_RUNNING"}'
        hbMsg = HeartbeatMessage("SupervisorWithKafka:SUPERVISOR@ias-fc40",
                                 IasHeartbeatStatus.PARTIALLY_RUNNING,
                                 None,
                                 "2025-01-03T09:58:56.641")
        json_str = hbMsg.toJSON()
        assert json_str == hbMsgStr

    def test_from_hb_with_props(self):
        hbMsgStr = '{"timestamp":"2025-01-03T11:05:57.361","hbStringrepresentation":"SupervisorID:SUPERVISOR@ias-fc40","state":"RUNNING","props":{"key1":"prop1","key2":"prop2"}}'
        hbMsg = HeartbeatMessage("SupervisorID:SUPERVISOR@ias-fc40",
                                 IasHeartbeatStatus.RUNNING,
                                 {"key1":"prop1","key2":"prop2"},
                                 "2025-01-03T11:05:57.361")
        json_str = hbMsg.toJSON()
        assert json_str == hbMsgStr
