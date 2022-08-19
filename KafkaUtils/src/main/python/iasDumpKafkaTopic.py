#! /usr/bin/env python
'''
Dump the strings published in the kafka topics
by delegating to kafka native commands.

No further computation is done on the received strings.
'''

import argparse
import os
import sys
from subprocess import run, DEVNULL, STDOUT, CalledProcessError, TimeoutExpired


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
    parser = argparse.ArgumentParser(description='Dumps string from a topic by delegating to kafka commands')
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
        choices=['core', 'hb', 'plugin', 'cmd', 'reply'],
        default = 'core',
        required=False)
    parser.add_argument(
        '-m',
        '--max-messages',
        help = 'The max number of messages to get from the topic (>0)',
        action='store',
        type = int,
        required = False)
    parser.add_argument(
        '-o',
        '--timeout-ms',
        help = 'The timeout (msec>0) while getting messages from the topic',
        action = 'store',
        type = int,
        required = False)
    parser.add_argument(
        '-q',
        '--quiet',
        help='Quiet mode (to use with --max-message or --timeout-ms)',
        action='store_true',
        default=False,
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
        sys.exit(1)

    topics = {
        'core':"BsdbCoreKTopic",
        'hb':"HeartbeatTopic",
        'plugin':"PluginsKTopic",
        'cmd':"CmdTopic",
        'reply':"ReplyTopic"}

    cmd = [ os.environ["KAFKA_HOME"]+kafkaCommand ,"--bootstrap-server" ]
    cmd.append(args.broker+":"+str(+args.port))
    cmd.append("--topic")
    cmd.append(topics[args.topic])

    if args.max_messages:
        max_messages = args.max_messages
        if max_messages <= 0:
            print("Invalid max messages",max_messages)
            sys.exit(1)
        cmd.append("--max-messages")
        cmd.append(str(max_messages))

    if args.timeout_ms:
        timeout = args.timeout_ms
        if timeout <= 0:
            print("Invalid timeout",timeout)
            sys.exit(1)
        #cmd.append("--timeout-ms")
        #cmd.append(str(timeout))
        to = timeout/1000
    else:
        to = None

     # Quiet can be used only with at least one betwee
     # --max-messages and --timeout-ms
    applyQuiet = args.quiet and (args.timeout_ms or args.max_messages)

    if args.allFromBeginning:
        cmd.append("--from-beginning")

    if args.verbose:
        print("Running",cmd)

    try:
        if applyQuiet:
            ret = run(cmd, stdout=DEVNULL, stderr=STDOUT, timeout=to)
        else:
            ret = run(cmd, timeout=to)
    except CalledProcessError:
        if not applyQuiet:
            sys.stderr.write('Exception\n')
        exit(1)
    except TimeoutExpired:
        if not applyQuiet:
            sys.stderr.write('TIMEOUT\n')
        exit(1)

