from enum import Enum

class IasHeartbeatStatus(Enum):
    """
    The heartbeat status (python equivalent of HeartbeatStatus.java)
    """
    STARTING_UP = 0,
    
    # The tool is up and running
    RUNNING = 1,
    
    # The tool is running but  not fully operative
    # 
    # For example a Supervisor where a DASU does not run
    # due to a failure of the TF of one of the ASCEs.
    PARTIALLY_RUNNING = 2,
    
    # The tool is paused i.e. it does not process inputs 
    # neither produces outputs.
    PAUSED = 3,
    
    # The tool is shutting down
    # 
    ## This message includes releasing of resources, closing of threads, etc.
    EXITING = 4

    @staticmethod
    def fromString(hBStatus: str):
        """
        Returns the IasHeartbeatStatus of the passed string
        Args:
            hBStatus: the string representation of the IasHeartbeatStatus
        """
        if not hBStatus:
            raise ValueError("Invalid string representation of the exit status of a comamnd")

        temp = str(hBStatus)
        if "." not in temp:
            temp="IasHeartbeatStatus."+temp
        for hbst in IasHeartbeatStatus:
            if str(hbst)==temp:
                return hbst
        # No enumerated matches with hbst
        raise NotImplementedError("Not supported/find IAS HB status: " + hBStatus)