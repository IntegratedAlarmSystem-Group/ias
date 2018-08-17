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
from IasBasicTypes.IasValue import IasValue
from IasSupport.KafkaValueConsumer import IasValueListener, KafkaValueConsumer

# The kafka consumer
consumer = None

# Signal that the user pressed CTRL+C
terminated = False

class DumperListener(IasValueListener):
    
    
    
    def __init__(self,verbose, toJson):
        """
        Constructor
        
        @param if True prints verbose messages
        @param if True print JSON string
        """
        self.verbose = verbose
        self.toJson = toJson
    
    def iasValueReceived(self,iasValue):
        """
        Print the IasValue in the stdout
        """
        if self.toJson:
            print(iasValue.toJSonString())
        else:
            print(iasValue.toString(verbose))

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
    listener = DumperListener(args.verbose,args.jsonformat)
    consumer = KafkaValueConsumer(
        listener, 
        args.kafkabrokers, 
        args.topic, 
        args.clientid, 
        args.groupid)
    
    consumer.start()
    try:
        consumer.join()
    except KeyboardInterrupt:
        pass
    

    