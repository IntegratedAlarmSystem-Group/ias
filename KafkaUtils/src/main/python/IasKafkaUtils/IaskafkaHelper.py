"""
A collection of methods to help dealing with kafka
"""

import os
import time

from confluent_kafka.admin import (AdminClient, NewTopic)

class IasKafkaHelper():

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
