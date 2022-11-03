#! /usr/bin/env python3
'''
Purge all the logs from a kafka topic.

IAS does not normally need to purge logs from topic but for testing
it can be better to start from an empty topic instead of getting messages
left over by a previous test

This is accomplished by deleting the topic with a native kafka command.

Prerequisite: delete.topic.enable=true
              must be set in the kafka server properties file
'''
import argparse
import os
import sys
from subprocess import call

from IasKafkaUtils import IaskafkaHelper

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Purge all the logs from a topic')
    parser.add_argument(
        '-b',
        '--bootstrapserver',
        help='The kafka bootstrap server (default localhost:9092)',
        action='store',
        default="localhost:9092",
        required=False)
    parser.add_argument(
        '-t',
        '--topic',
        help='The IAS topic to purge logs from',
        action='store',
        choices=['core', 'hb', 'plugin', 'cmd', 'reply'],
        default = 'core',
        required=False)
    parser.add_argument(
        '-v',
        '--verbose',
        help='Verbose mode (default no verbose)',
        action='store_true',
        default=False,
        required=False)

    args = parser.parse_args()

    kafkaCommand = "bin/kafka-topics.sh"
    if not IaskafkaHelper.IasKafkaHelper.check_kafka_cmd(kafkaCommand):
        print (f"ERROR: kafka command {kafkaCommand} NOT found")
        sys.exit(-1)

    cmd = [ os.environ["KAFKA_HOME"]+f"{os.sep}{kafkaCommand}", "--bootstrap-server" ]
    cmd.append(args.bootstrapserver)
    cmd.append("--delete")
    cmd.append("--if-exists")
    cmd.append("--topic")
    cmd.append(IaskafkaHelper.IasKafkaHelper.topics[args.topic])

    if args.verbose:
        print("Running",cmd)

    call(cmd)

