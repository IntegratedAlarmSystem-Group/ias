"""
Configuration for the GUI can be taken from command line parameters, the CDB or environment variables
"""
import os

from IASLogging.logConf import Log

from IasCdb.CdbReader import CdbReader

class Config:
    """
    Helper class to handle configuration

    Parameters passed through the command line takes precedence over those read from environment variables    
    """
    
    # The path of the CDB read from the environment variable or None if not defined
    CDB_FROM_ENV: str|None = os.environ.get("IAS_CDB")

    # The URL of the kafka brokers from the environment or None if not defined
    BSDB_URL_ENV: str|None = os.environ.get("KAFKA_BROKERS")

    

    def __init__(self, ias_cdb_cmd_line: str|None) -> None:
        """
        Constructor
        
        Params:
            ias_cdb: The parent folder of the CDB read from the command line if availabel,
                     None otherwise
        """
        # The logger
        self.logger = Log.getLogger(__name__)

        # The CDB folder read from the comamnd line of from environment variable
        # It is None if no CDB folder can be retrieved from command line or the environment
        self._ias_cdb: str|None = ias_cdb_cmd_line if ias_cdb_cmd_line else Config.CDB_FROM_ENV

        self.logger.debug("IAS CDB path is %s", self._ias_cdb)

    def get_cdb(self) -> str|None:
        """
        Return:
            The path of the CDB if available from command line or the environment,
            None otherwise
        """
        return self._ias_cdb
    
    def read_bsdb_url_from_cdb(self) -> str|None:
        """
        Read and return the URL from the CDB

        Returns:
            The URL of the kafka brokers read from the CDB
        Raises:
            ValueError: if the CDB cannot be read
        """
        if not self._ias_cdb:
            raise ValueError("IAS CDB path is not available")
        reader = CdbReader(self._ias_cdb)
        ias = reader.get_ias()
        return ias.bsdb_url
    
    def get_bsdb_url(self, url_from_cmd_line: str|None, default_bsdb: str|None=None) -> str|None:
        """
        Get the URL of the kafka brokers.

        Params:
            url_from_cmd_line: the URL read from the command line if available,
                               None otherwise
            default_bsdb: the default URL to return if no URL is available from
                           command line, CDB or environment variable

        Returns:
            The URL of the KAFKA brokers retrieved from
                - command line, if available
                - CDB, if available
                - environment variable, if available       
                - None otherwise
        Raises:
            ValueError: if the IAS_CDB is defined but the CDB cannot be read
        """
        if url_from_cmd_line:
            self.logger.debug("Using BSDB URL from command line: %s", url_from_cmd_line)
            return url_from_cmd_line
        
        if self._ias_cdb:
            self.logger.debug("Reading BSDB URL from CDB")
            url = self.read_bsdb_url_from_cdb()
            self.logger.debug("Using BSDB URL from CDB: %s", url)
            return url
        
        self.logger.debug("Using BSDB URL from environment variable: %s", Config.BSDB_URL_ENV)
        if Config.BSDB_URL_ENV:
            return Config.BSDB_URL_ENV

        self.logger.debug("Using default BSDB URL: %s", default_bsdb)
        return default_bsdb
        