"""
A collection of utility functions for the Extras module
"""
import logging
import os

from IasCdb.CdbReader import CdbReader

class Utils():
    """
    A class with helpper functions
    """

    # The logger
    logger = logging.getLogger(__name__)

    @classmethod
    def get_bsdb_from_cdb(cls, cdb_folder: str) -> str :
        """
        Get the BSDB URL from the CDB
        
        :param cdb_folder: The parend folder of the CDB
        :type cdb_folder: str
        :return: The URL to connect to the BSDB read from the IAS CDB
        :rtype: str
        """
        cls.logger.debug("Getting BSDB URL from IAS CDB")
        cdb_reader = CdbReader(cdb_folder)
        ias_dao = cdb_reader.get_ias()
        return ias_dao.bsdb_url

    @classmethod
    def get_bsdb_url(cls, kafka_brokers, jCdb) -> str :
        """
        Get the BSDB URL from (whatever is true)
        - the kafka brokers passed in the command line
        - the IAS CDB passed in the command line
        - the IAS_CDB from the environment variable, if defined
        - default
        
        :param kafka_brokers: kafka brokers URL from command line
        :param jCdb: Description CDB folder from the command line
        :return: The URL to connect to the BSDB
        :rtype: str
        """
        if kafka_brokers is not None:
            cls.logger.debug("Using kafka brokers from command line")
            return kafka_brokers
        elif jCdb is not None:
            cls.logger.debug("Using kafka brokers from IAS CDB passed in command line")
            return cls.get_bsdb_from_cdb(jCdb)
        elif os.getenv("IAS_CDB") is not None:
            cls.logger.debug("Using kafka brokers from IAS_CDB environment variable")
            # get the kafka brokers from the IAS_CDB env var, if defined
            return cls.get_bsdb_from_cdb(os.environ["IAS_CDB"])
        else:
            return "localhost:9092"