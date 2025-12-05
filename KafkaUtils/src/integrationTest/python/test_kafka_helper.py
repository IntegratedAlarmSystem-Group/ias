#! /usr/bin/env python3
'''
Test the IasKafkaHelper
'''
import random
import string
from IASLogging.logConf import Log
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper


def generate_random_string(length=10):
    """
    Generate a random string of the given length composed 
    upper and lower letters and numbers
    """
    characters = string.ascii_letters + string.digits  # A-Z, a-z, 0-9
    return ''.join(random.choice(characters) for _ in range(length))


class TestKafkaHelper():
    LOGGER = Log.getLogger(__name__)
    KAFKA_BROKERS = IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS

    def test_topic_creation(self):
        '''
        Check the creation of a topic that does not yet already exist
        '''
        TestKafkaHelper.LOGGER.debug("Test topic creation")

        topic_name = generate_random_string(16)
        TestKafkaHelper.LOGGER.info("Creating topic with random name %s", topic_name)

        # Ensure the topic does not yet exist
        assert not IasKafkaHelper.topicExists(topic_name, TestKafkaHelper.KAFKA_BROKERS)

        # Create the topic
        assert IasKafkaHelper.createTopic(topic_name, TestKafkaHelper.KAFKA_BROKERS)
        TestKafkaHelper.LOGGER.info("Topic %s created", topic_name)

        # Ensure the topic exists
        assert IasKafkaHelper.topicExists(topic_name, TestKafkaHelper.KAFKA_BROKERS)

        # Delete the topic
        TestKafkaHelper.LOGGER.info("Topic %s deleted", topic_name)
        assert IasKafkaHelper.deleteTopic(topic_name, TestKafkaHelper.KAFKA_BROKERS)
