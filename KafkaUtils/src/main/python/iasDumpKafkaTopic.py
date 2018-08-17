#! /usr/bin/env python
'''
Dump the strings published in the kafka topics
by delegating to kafka native commands.

No further computation is done on the received strings.
'''

import argparse, os, sys
from subprocess import call

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
        help='The kafka broker to connect to (default localhost)',
        action='store',
        default="localhost",
        required=False)
    parser.add_argument(
        '-p',
        '--port',
        help='The port of the kafka broker to connect to (default 9092)',
        action='store',
        default=9092,
        type=int,
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
        choices=['core', 'hb', 'plugin'],
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
    if not check_kafka(kafkaCommand):
        sys.exit(-1)

    topics = { 'core':"BsdbCoreKTopic", 'hb':"HeartbeatTopic", 'plugin':"PluginsKTopic" }

    cmd = [ os.environ["KAFKA_HOME"]+kafkaCommand ,"--bootstrap-server" ]
    cmd.append(args.broker+":"+str(+args.port))
    cmd.append("--topic")
    cmd.append(topics[args.topic])

    if args.allFromBeginning:
        cmd.append("--from-beginning")

    if args.verbose:
        print("Running",cmd)

    call(cmd)
