"""
A collection of methods to help dealing with kafka
"""

import os

class IasKafkaHelper(object):

    # Associates human readable topic names with their names in kafka
    topics = {
        'core': "BsdbCoreKTopic",
        'hb': "HeartbeatTopic",
        'plugin': "PluginsKTopic",
        'cmd': "CmdTopic",
        'reply': "ReplyTopic"}

    @classmethod
    def check_kafka_cmd(cls, kafkaCommand):
        '''
        Check if kafka commands exists
        :rtype: bool
        :param kafkaCommand: kafkaCommand the command to run from KAFKA_HOME
        :return: True if kafka is available
        '''
        try:
            kafkaHome = os.environ["KAFKA_HOME"]
        except:
            print("KAFKA_HOME environment variable not defined")
            return False

        if not os.access(kafkaHome+os.sep+kafkaCommand, os.X_OK):
            return False
        return True
