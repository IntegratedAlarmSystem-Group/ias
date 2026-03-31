"""
A collection of methods to help dealing with kafka
"""

import os
import time
import logging
from pathlib import Path

from confluent_kafka.admin import (AdminClient, NewTopic)

from IasCdb.CdbReader import CdbReader

class IasKafkaHelper():

    # The logger
    logger = logging.getLogger(__name__)

    # Associates human readable topic names with their names in kafka
    topics = {
        'core': "BsdbCoreKTopic",
        'hb': "HeartbeatTopic",
        'plugin': "PluginsKTopic",
        'cmd': "CmdTopic",
        'reply': "ReplyTopic"}
    
    # Default list of servers
    DEFAULT_BOOTSTRAP_BROKERS="localhost:9092"
    
    @classmethod
    def topicExists(cls, topicName: str, kafkaBrokers: str) -> bool:
        """
        Check if a topic with the given name exists

        Args:
            topicName: the name of the topic to check
            kafkaBrokers: the kafka brokers
        Return:
          True if the topic exists; False otherwise
        """
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        metadata = admin.list_topics()
        return topicName in metadata.topics

    @classmethod
    def createTopic(cls, 
        topicName: str, 
        kafkaBrokers: str, 
        partitions: int=8, 
        timeout=30) -> bool:
        """
        Create a topic

        It delegates to the AdminClient for creating the topic with the given name

        Args:
            topicName: the name of the topic to create
            kafkaBrokers: the kafka brokers
            partitions: the number of partitions to create in the topic
            timeout: timeout (seconds) for the broker to delete the topic
        Return:
          True if the topic was created or already exists; False otherwise
        """
        if IasKafkaHelper.topicExists(topicName, kafkaBrokers):
            return True
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        new_topic = NewTopic(topic=topicName, num_partitions=partitions) 
        result_dict = admin.create_topics([new_topic])

        # Wait for operation to finish by getting the existing topics
        now = int(time.time() * 1000)
        end_time = now + int(timeout * 1000)
        while now <= end_time:
            now = int(time.time() * 1000)
            if IasKafkaHelper.topicExists(topicName, kafkaBrokers):
                return True
            time.sleep(0.25)
        return False
    
    @classmethod
    def deleteTopic(cls, topicName: str, kafkaBrokers: str, timeout=30) -> bool:
        """
        Delete a topic

        It delegates to the AdminClient for deleting the topic with the given name.

        The AdminClient deletes the topic asynchronously and uses the passed
        timeout before giving up in case of problems.
        
        This function checks if the topic has been deleted by checking the
        existing topics until the timeout expires.

        Args:
            topicName: the name of the topic to create
            kafkaBrokers: the kafka brokers
            timeout: timeout (seconds) for the broker to delete the topic
        Return:
          True if the topic was delete or does not exists; False otherwise
        """
        if not IasKafkaHelper.topicExists(topicName, kafkaBrokers):
            return True
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        # admin.delete_topics(..) returns immediately, 
        # the operation_timeout applies to the broker
        fs = admin.delete_topics([topicName], operation_timeout=timeout)

        # Wait for operation to finish by getting the existing topics
        now = int(time.time() * 1000)
        end_time = now + int(timeout * 1000)
        while now <= end_time:
            now = int(time.time() * 1000)
            if not IasKafkaHelper.topicExists(topicName, kafkaBrokers):
                return True
            time.sleep(0.25)
        return False
            
    @classmethod
    def getTopicNames(cls,kafkaBrokers: str) -> list[str]:
        """
        Returns:
          The list of the topic names
        """
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        metadata = admin.list_topics()
        ret = []
        for t in iter(metadata.topics.values()):
            ret.append(t.topic)
        return ret

    @classmethod
    def get_bsdb_from_cdb(cls, cdb_parent_folder: str) -> str :
        """
        Get the BSDB URL from the CDB
        
        :param cdb_parent_folder: The parend folder of the CDB
        :type cdb_parent_folder: str
        :return: The URL to connect to the BSDB read from the IAS CDB
        :rtype: str
        :raises: ValueError if the CDB parent folder is is not readable or
                            does not exist
        """
        cls.logger.debug("Getting BSDB URL from IAS CDB %s", cdb_parent_folder)
        # Check if the cdbfolder exists and contains CDB
        cdb_path = Path(cdb_parent_folder) / "CDB"
        if not cdb_path.is_dir():
            raise ValueError(f"Invalid CDB folder {cdb_parent_folder}")
        cdb_reader = CdbReader(cdb_parent_folder)
        ias_dao = cdb_reader.get_ias()
        return ias_dao.bsdb_url

    @classmethod
    def get_bsdb_url(cls, kafka_brokers, jCdb) -> str :
        """
        Get the BSDB URL from whatever is true first among:
        - the kafka brokers passed in the command line
        - the IAS CDB passed in the command line (jCdb)
        - the IAS_CDB from the environment variable, if defined
        - default
        
        :param kafka_brokers: kafka brokers URL from command line
        :param jCdb: The CDB folder from the command line
        :return: The URL to connect to the BSDB
        :rtype: str
        """
        if kafka_brokers is not None:
            cls.logger.debug("Using kafka brokers from command line")
            return kafka_brokers
        
        if jCdb is not None:
            cls.logger.debug("Using kafka brokers from IAS CDB passed in command line")
            try:
                return cls.get_bsdb_from_cdb(jCdb)
            except Exception as e:
                cls.logger.error("Invalid CDB folder from command line %s", jCdb)
        
        if os.getenv("IAS_CDB") is not None:
            cls.logger.debug("Using kafka brokers from IAS_CDB environment variable")
            # get the kafka brokers from the IAS_CDB env var, if defined
            try:
                return cls.get_bsdb_from_cdb(os.environ["IAS_CDB"])
            except Exception as e:
                cls.logger.error("Invalid CDB folder from environment %s", jCdb)

        cls.logger.info("BSDB URL not found: using default %s", cls.DEFAULT_BOOTSTRAP_BROKERS)
        return cls.DEFAULT_BOOTSTRAP_BROKERS
