from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus

class Heartbeat:
    """
    The python Heartbeat equivalent to Heartbeat.scala
    """
    # Separator between type and name
    typeNameSeparator = ':'

    # The seprator between the ID and the hostname
    idHostnameSeparator = '@'

    def __init__(self, hbType: HeartbeatProducerType, name: str, hostName: str):
        """
        Constructor
        Args:
            hbType The type of this HB
            name: The name of the sender of the HB
            hostname: The hostname of the sender of the HB
        """
        self.hbType = hbType
        self.name = name
        self.hostname = hostName

        # The ID is composed of the type and the name
        self.id: str = self.name+Heartbeat.typeNameSeparator+self.hbType.name

        # The string representation of the heartbeat
        # The representation is sent in the HB topic and returned by toString
        self.stringRepr: str = self.id+Heartbeat.idHostnameSeparator+self.hostname

    @classmethod
    def fromStringRepr(cls, strRepr: str):
        """
        Build a IasHeartbeat from the string representation
        Params:
            "ias-fc40" The string representation of the HB
        """
        if not strRepr:
            raise ValueError("Invalid null HB string representation")
        
        parts = strRepr.split(Heartbeat.idHostnameSeparator)
        if len(parts)!=2:
            raise ValueError(f"Invalid HB string representation format: {strRepr}")
        host = parts[1]
        id = parts[0]
        parts = id.split(Heartbeat.typeNameSeparator)
        if len(parts)!=2:
            raise ValueError(f"Invalid HB string representation format: {strRepr}")
        hbType = HeartbeatProducerType.fromString(parts[1])
        name = parts[0]

        return Heartbeat(hbType, name, host)
        