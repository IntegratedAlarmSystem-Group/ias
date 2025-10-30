#! /usr/bin/env -S python3 -u
'''
Dumps in the stdout the logs published in a BSDB topic

Created on Apr 11, 2024

@author: acaproni
'''
import argparse
import sys
import signal
from IasKafkaUtils.IasKafkaConsumer import IasLogListener, IasLogConsumer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from threading import Lock

# The consumer
consumer = None

# Signal that the user pressed CTRL+C
terminated = False

# The lock for the termination
termination_lock = Lock()

class DumperListener(IasLogListener):

    def __init__(self, quiet: bool, max_messages: int) -> None:
        """
        Constructor

        Params:
            quiet: True if the logs must not be printed in the stdout
                   (to be used with -m or -o)
            max_messages: the max number of messages to wait
                          (0 means unlimited)
        """
        super().__init__()
        self.__quiet = quiet
        self.__max_messages = max_messages
        self.__logs_processed = 0
        # The lock for the mutual exclusion
        self.__mutex = Lock()
    
    def iasLogReceived(self, log: str) -> None:
        """
        Process the log
        """
        self.__mutex.acquire()
        if self.__max_messages>0 and self.__logs_processed>=self.__max_messages:
            if termination_lock.locked():
                termination_lock.release()
        else:
            self.__logs_processed = self.__logs_processed + 1
            if not self.__quiet:
                print(log)
        self.__mutex.release()

    def get_processed_logs(self):
        '''
        Returns: the numberof logs procesed so far
        '''
        self.__mutex.acquire()
        ret = self.__logs_processed
        self.__mutex.release()
        return ret

def interrupt_handler(signum, frame):
    '''
    The handler of CTRL-C
    '''
    termination_lock.release()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Dumps kafla logs published in the BSDB or a custom topic. Unless -m or -o is set, the tool must be terminated with CTRL=C')
    parser.add_argument(
                        '-k',
                        '--kafkabrokers',
                        help='The kafka bootstrap servers to connect to (default localhost:9092)',
                        action='store',
                        default='localhost:9092',
                        required=False)
    parser.add_argument(
                        '-b',
                        '--bsdb_topic',
                        help='The name of the BSDB topic to connect to',
                        action='store',
                        choices=['core', 'hb', 'plugin', 'cmd', 'reply'],
                        required=False)
    parser.add_argument(
                        '-n',
                        '--topic_name',
                        help='The name of the kafka topic to connect to (thought for testing to get logs for non standard BSDB topics)',
                        action='store',
                        required=False)
    parser.add_argument(
                        '-c',
                        '--clientid',
                        help='Kafka group ID (default iasLogDumper)',
                        action='store',
                        default="iasLogDumper-client",
                        required=False)
    parser.add_argument(
                        '-g',
                        '--groupid',
                        help='Kafka group ID',
                        action='store',
                        default="iaslogDumper-group",
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
                        default=-1000,
                        required = False)
    parser.add_argument(
                        '-q',
                        '--quiet',
                        help='Quiet mode (to use with --max-message or --timeout-ms)',
                        action='store_true',
                        default=False,
                        required=False)
    
    args = parser.parse_args()
    if args.bsdb_topic is None and args.topic_name is None:
        print("ERROR: one option between topic_name and bsdb_topic must be set in the command line")
        sys.exit(-1)

    if args.max_messages is not None:
        max_messages = args.max_messages
        if max_messages <= 0:
            print(" Max messages must be >0")
            sys.exit(1)
    else:
        max_messages = 0 # No limit

    listener = DumperListener(args.quiet, max_messages)

    if args.bsdb_topic is not None:
        topic = IasKafkaHelper.topics[args.bsdb_topic]
    else:
        topic = args.topic_name

    consumer = IasLogConsumer(
        listener, 
        args.kafkabrokers, 
        topic, 
        args.clientid, 
        args.groupid)
    
    termination_lock.acquire()
    signal.signal(signal.SIGINT, interrupt_handler)

    consumer.start()

    termination_lock.acquire(timeout=args.timeout_ms/1000)
    consumer.close()
    consumer.join()

    print(listener.get_processed_logs(), "logs processed")
