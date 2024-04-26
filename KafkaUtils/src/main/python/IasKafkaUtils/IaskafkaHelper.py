"""
A collection of methods to help dealing with kafka
"""

import os

from confluent_kafka.admin import (AdminClient, NewTopic)

class IasKafkaHelper():

    # Associates human readable topic names with their names in kafka
    topics = {
        'core': "BsdbCoreKTopic",
        'hb': "HeartbeatTopic",
        'plugin': "PluginsKTopic",
        'cmd': "CmdTopic",
        'reply': "ReplyTopic"}
    
    @classmethod
    def topicExists(cks, topicName: str, kafkaBrokers: str) -> bool:
        """
        Check if a topic with the given name exists

        Args:
            topicName: the neame of the topic to check
            kafkaBrokers: the kafka brokers
        Return:
          True if the topic exists; False otherwise
        """
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        metadata = admin.list_topics()
        return topicName in metadata.topics

    @classmethod
    def createTopic(cls, topicName: str, kafkaBrokers: str):
        """
        Create a topic

        It delegates to the AdminClient for creating the topic with the given name

        Args:
            topicName: the neame of the topic to create
            kafkaBrokers: the kafka brokers
        Return:
          True if the topic was created or already exists; False otherwise
        """
        if IasKafkaHelper.topicExists(topicName, kafkaBrokers):
            return True
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        #new_topic = NewTopic(topic, num_partitions=6, replication_factor=3) 
        new_topic = NewTopic(topicName) 
        result_dict = admin.create_topics([new_topic])
        for topic, future in result_dict.items():
            try:
                future.result()  # The result itself is None
                print("Topic {} created".format(topic))
                return True
            except Exception as e:
                print("Failed to create topic {}: {}".format(topic, e))
                return False
    
    @classmethod
    def deleteTopic(cls, topicName: str, kafkaBrokers: str):
        """
        Create a topic

        It delegates to the AdminClient for creating the topic with the given name

        Args:
            topicName: the neame of the topic to create
            kafkaBrokers: the kafka brokers
        Return:
          True if the topic was delete or does not exists; False otherwise
        """
        if not IasKafkaHelper.topicExists(topicName, kafkaBrokers):
            return True
        admin = AdminClient({'bootstrap.servers': kafkaBrokers})
        fs = admin.delete_topics([topicName], operation_timeout=30)

        # Wait for operation to finish.
        for topic, f in fs.items():
            try:
                f.result()  # The result itself is None
                print("Topic {} deleted".format(topic))
                return True
            except Exception as e:
                print("Failed to delete topic {}: {}".format(topic, e))
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
