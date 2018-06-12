#! /usr/bin/env python
'''
Dumps in the stdout the IasValue published in the
BSDB

Created on Jun 12, 2018

@author: acaproni
'''
import argparse
from kafka import KafkaConsumer
from IasSupport.IasValue import IasValue

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
    print(args)
    consumer = KafkaConsumer(
        args.topic,
        bootstrap_servers=args.kafkabrokers,
        client_id=args.clientid,
        group_id=args.groupid,
        auto_offset_reset="latest")
    for msg in consumer:
        json = msg.value.decode("utf-8")
        if args.jsonformat:
            print(json)
        else:
            iasValue = IasValue.fromJSon(json)
            print(iasValue.toString(args.verbose))
    
    