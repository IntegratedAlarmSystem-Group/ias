"""
A collection of methods to help dealing with kafka
"""

import os

class IasKafkaHelper():

    # Associates human readable topic names with their names in kafka
    topics = {
        'core': "BsdbCoreKTopic",
        'hb': "HeartbeatTopic",
        'plugin': "PluginsKTopic",
        'cmd': "CmdTopic",
        'reply': "ReplyTopic"}
