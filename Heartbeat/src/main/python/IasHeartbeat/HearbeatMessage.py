import json

from IasHeartbeat.HeartbeatStatus import HeartbeatStatus

class HeartbeatMessage:
    """
    The HB message published in the kafka topic
    It is the python equivalent of HeartbeatMessagePojo.java
    """

    def __init__(self,
                 hbStringrepRepr: str,
			     hbStatus: HeartbeatStatus,
			     props: dict[str, str],
			     tStamp: str):
        """
        Constructor
        Params:
            hbStringRepr: the string representation of the IasHeartbeat
            hbStatus: the status of the heartbeat
            props: the properties of the heartbeat (can be None, empty)
            tStamp: the ISO8601 timestamp (string) of the heartbeat 
        """
        self.hbStringrepresentation = hbStringrepRepr
        self.state = hbStatus
        self.props = props
        self.timestamp = tStamp

    def toJSON(self) -> str:
        """
        Convert the HeartbeatMessage to a JSON string

        Returns:
           A Json string representing the HeartbeatMessage
        """
        ret = {
            "timestamp":self.timestamp,
            "hbStringrepresentation":self.hbStringrepresentation,
            "state":self.state.name
        }
        if self.props:
            ret["props"] = self.props
        return json.dumps(ret)

    @classmethod
    def fromJSON(cls, json_str: str):
        """
        Returns:
            The HeartbeatMessage whose representation is given by the passed JSON string
        """
        if not json_str:
            raise ValueError("Cannot deserialize an empty string")
        j = json.loads(json_str)
        return HeartbeatMessage(
            j["hbStringrepresentation"],
            HeartbeatStatus.fromString(j["state"]),
            j.get("props", None),
            j["timestamp"]
        )
