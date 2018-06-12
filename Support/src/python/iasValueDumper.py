#! /usr/bin/env python
'''
Dumps in the stdout the IasValue published in the
BSDB

Created on Jun 12, 2018

@author: acaproni
'''
import argparse
import signal, sys
from kafka import KafkaConsumer
from IasSupport.IasValue import IasValue

# The kafka consumer
consumer = None

# Signal that the user pressed CTRL+C
terminated = False

def signal_handler(signal, frame):
    global consumer
    global terminated
    terminated = True
    print()
    if consumer is not None:
        consumer.close()
        sys.exit(0)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Dumps IasValue published in the BSDB')
    parser.add_argument(
                        '-k',
                        '--kafkabrokers',
                        help='The kafka bootstrap servers to connect to like iasdevel.hq.eso.org:9092',
                        action='store',
                        required=True)
    parser.add_argument(
                        '-t',
                        '--topic',
                        help='The kafka bootstrap servers to connect to like iasdevel.hq.eso.org:9092',
                        action='store',
                        default="BsdbCoreKTopic",
                        required=False)
    parser.add_argument(
                        '-c',
                        '--clientid',
                        help='Kafka group ID',
                        action='store',
                        default="iasValueDumper",
                        required=False)
    parser.add_argument(
                        '-g',
                        '--groupid',
                        help='Kafka group ID',
                        action='store',
                        default="iasValueDumper-group",
                        required=False)
    parser.add_argument(
                        '-j',
                        '--jsonformat',
                        help='prints the JSON representation of the IasValue',
                        action='store_true',
                        default=False,
                        required=False)
    parser.add_argument(
                        '-v',
                        '--verbose',
                        help='Verbose messages',
                        action='store_true',
                        default=False,
                        required=False)
    args = parser.parse_args()
    consumer = KafkaConsumer(
        args.topic,
        bootstrap_servers=args.kafkabrokers,
        client_id=args.clientid,
        group_id=args.groupid,
        auto_offset_reset="latest",
        consumer_timeout_ms=1000,
        enable_auto_commit=True)
    
    print()
    
    # Register the handler of CTRL+C
    #signal.signal(signal.SIGINT, signal_handler)
    
    while True:
        try:
            messages = consumer.poll(500)
        except:
            KeyboardInterrupt
            break
        for listOfConsumerRecords in messages.values():
            for cr in listOfConsumerRecords:
                json = cr.value.decode("utf-8")
                if args.jsonformat:
                    print(json)
                else:
                    iasValue = IasValue.fromJSon(json)
                    print(iasValue.toString(args.verbose))
    