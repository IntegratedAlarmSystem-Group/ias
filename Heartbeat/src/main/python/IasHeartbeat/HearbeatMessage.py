import json

from IasHeartbeat.IasHeartbeat import IasHeartbeat
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus

class HeartbeatMessage:
    """
    The HB message published in the kafka topic
    It is the python equivalent of HeartbeatMessagePojo.java
    """

    def __init__(self,
                 hbStringrepRepr: str,
			     hbStatus: IasHeartbeatStatus,
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

        json.dunps() does not work in this case beacuse python.json only converts
        basic python data types (i.e. it fails with a dict property)

        Returns:
           A Json string representing the HeartbeatMessage
        """
        ret = f'"timestamp":"{self.timestamp}","hbStringrepresentation":"{self.hbStringrepresentation}", "state":"{self.state}"'
        if self.props:
            jsonDict = json.dumps(self.props)
            ret = f'{ret},"props":{jsonDict}'
        return f'{{{ret}}}'

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
            j["state"],
            j.get("props", None),
            j["timestamp"]
        )
