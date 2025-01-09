from enum import Enum

class IasHeartbeatProducerType(Enum):
    # Plugin
    PLUGIN = 0,

    # Converter
    CONVERTER = 1,

    # Supervisor
    SUPERVISOR = 2,

    # Generic client like an engineering client.
    # Not necessarily the core monitors this kind of clients.
    CLIENT = 3,

    # A consumer of IASIOs
    SINK = 4,

    # A core tool that can generate alarms other than a DASU
    #
    # The IAS monitor is one of such tools.
    CORETOOL=5

    @staticmethod
    def fromString(prodType: str):
        """
        Returns the IasHeartbeatStatus of the passed string
        Args:
            hBStatus: the string representation of the IasHeartbeatStatus
        """
        if not prodType:
            raise ValueError("Invalid string representation of the exit status of a comamnd")

        temp = str(prodType)
        if "." not in temp:
            temp="IasHeartbeatProducerType."+temp
        for hbst in IasHeartbeatProducerType:
            if str(hbst)==temp:
                return hbst
        # No enumerated matches with hbst
        raise NotImplementedError("Not supported/find IAS HB producer type: " + prodType)