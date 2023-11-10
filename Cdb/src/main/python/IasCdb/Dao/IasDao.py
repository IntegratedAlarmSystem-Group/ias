class IasDao():
    """
    Global configuration for the IAS
    """
	
    def __init__(self, cdb_path: str):
        """
        Constructor
        Reads the ias.json or ias.yaml from the CDB
        """
        self.cdb = cdb_path
        if not self.cdb:
            raise Exception("Invalid CDB")
        self.logLevel = None #LogLevel
        self.props = [] #PropertyDao
        self.refreshRate = None # int
        self.validityThreshold = None #int
        self.hbFrequency = None # int
		
        # The URL to connect to the BSDB.
        # In case of kafka it is a comma separated list of server:port
        self.bsdbUrl = None # str
        self.smtp = None # str
        self.props = {} # Map of <name, value>

    def _read(self):
        """
        Read the XML or YAML configuration file
        of the IAS
        """
        file_name = 
	
    def __str__(self):
        """
        A string representatin of the IasDao
        """
        ret = f"IAS=[logLevel={self.logLevel}, refreshRate={self.refreshRate}"
        ret = ret + f", validityThreshold={self.validityThreshold}, heartebeat frequency={self.hbFrequency}"
        ret = ret + f", BSDB URL='{self.bsdbUrl}'"
        if self.smtp is not None:
            ret = ret + f", SMTP={self.smtp}"
        ret = ret + f", props={self.props}"
        return ret
