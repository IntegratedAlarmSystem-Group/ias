#! /usr/bin/env python3
'''
Dump the strings published in the kafka topics
by delegating to kafka native commands.

No further computation is done on the received strings.
'''

import argparse
import os
import sys
from subprocess import call

from IasKafkaUtils import IaskafkaHelper


def check_kafka(kafkaCommand):
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

    if not os.access(kafkaHome+kafkaCommand,os.X_OK):
        print("Cannot execute "+kafkaHome+"/"+kafkaCommand)
        return False
    return True

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Dumps string froma topic by delegating to kafka commands')
    parser.add_argument(
        '-b',
        '--broker',
        help='The kafka broker to connect to (default localhost:9092)',
        action='store',
        default="localhost:9092",
        required=False)
    parser.add_argument(
        '-a',
        '--allFromBeginning',
        help='Dumps all the values in the topic, from the beginning (default from the end)',
        action='store_true',
        default=False,
        required=False)
    parser.add_argument(
        '-t',
        '--topic',
        help='The IAS topic to get strings from',
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

    kafkaCommand = "/bin/kafka-console-consumer.sh"
    if not IaskafkaHelper.IasKafkaHelper.check_kafka_cmd(kafkaCommand):
        print (f"ERROR: kafka command {kafkaCommand} NOT found")
        sys.exit(-1)

    cmd = [ os.environ["KAFKA_HOME"]+kafkaCommand ,"--bootstrap-server" ]
    cmd.append(args.broker)
    cmd.append("--topic")
    cmd.append(IaskafkaHelper.IasKafkaHelper.topics[args.topic])

    if args.allFromBeginning:
        cmd.append("--from-beginning")

    if args.verbose:
        print("Running",cmd)

    call(cmd)
