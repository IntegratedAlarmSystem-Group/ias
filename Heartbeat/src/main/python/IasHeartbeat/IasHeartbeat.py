from IasHeartbeat.IasHeartbeatProducerType import IasHeartbeatProducerType
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus

class IasHeartbeat:
    """
    The python Heartbeat equivalent to Heartbeat.scala
    """
    # Separator between type and name
    typeNameSeparator = ':'

    # The seprator between the ID and the hostname
    idHostnameSeparator = '@'

    def __init__(self, hbType: IasHeartbeatProducerType, name: str, hostName: str):
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
        self.id: str = self.name+IasHeartbeat.typeNameSeparator+self.hbType.name

        # The string representation of the heartbeat
        # The representation is sent in the HB topic and returned by toString
        self.stringRepr: str = self.id+IasHeartbeat.idHostnameSeparator+self.hostname

    @classmethod
    def fromStringRepr(cls, strRepr: str):
        """
        Build a IasHeartbeat from the string representation
        Params:
            "ias-fc40" The string representation of the HB
        """
        if not strRepr:
            raise ValueError("Invalid null HB string representation")
        
        parts = strRepr.split(IasHeartbeat.idHostnameSeparator)
        if len(parts)!=2:
            raise ValueError(f"Invalid HB string representation format: {strRepr}")
        host = parts[1]
        id = parts[0]
        parts = id.split(IasHeartbeat.typeNameSeparator)
        if len(parts)!=2:
            raise ValueError(f"Invalid HB string representation format: {strRepr}")
        hbType = IasHeartbeatProducerType.fromString(parts[1])
        name = parts[0]

        return IasHeartbeat(hbType, name, host)
        