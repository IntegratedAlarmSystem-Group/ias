#! /usr/bin/env -S python3 -u
"""
Push an IASIO in the BSDB

This script builds and pushes an IASIO whose definition is passed in the command line.
It is meant to be used for testing and debugging purposes.
"""

import argparse
import uuid
import logging

from IasBasicTypes.IasType import IASType
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Validity import Validity
from IasBasicTypes.OperationalMode import OperationalMode
from IasLogging.log import Log
from IasKafkaUtils.KafkaValueProducer import KafkaValueProducer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasBasicTypes.Identifier import Identifier
from IasExtras.Utils import Utils

# The logger, intialized by main()
logger = None

def parse_args():
    epilog = """
Example usage:
    iasPushIasio -i "(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)" -t LONG -v 5
    iasPushIasio -i "(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)" -t ALARM -v "SET_ACK:HIGH"
    """


    parser = argparse.ArgumentParser(
        description="Push an IASIO to the BSDB",
        epilog=epilog,
        formatter_class=argparse.RawTextHelpFormatter)
    uuid_id = str(uuid.uuid4())
    parser.add_argument(
                        '-k',
                        '--kafkabrokers',
                        help='The kafka bootstrap servers to connect to (default to localhost:9092 unless -j/--jCdb is passed or $IAS_CDB env var exists)',
                        action='store',
                        default=None,
                        required=False)
    parser.add_argument(
                        '-j',
                        '--jCdb',
                        help='The IAS CDB',
                        action='store',
                        default=None,
                        required=False)
    parser.add_argument(
                        '-b',
                        '--bsdbid',
                        help='BSDB client ID used by Producer and Consumer (default iasPushIasio-client-<UUID>)',
                        action='store',
                        default=f"iasPushIasio-client-{uuid_id}",
                        required=False)
    parser.add_argument(
                        '-i',
                        '--iasioid',
                        help = 'The full running ID of the IASIO to push',
                        action='store',
                        type=str,
                        required=True)
    parser.add_argument(
                        '-t',
                        '--iasiotype',
                        help='The type of the IASIO to push',
                        action='store',
                        type=str,
                        choices=[
                            "LONG", 
                            'INT',
                            'SHORT',
                            'BYTE',
                            'DOUBLE',
                            'FLOAT',
                            'BOOLEAN', 
                            'CHAR',
                            'STRING',
                            'TIMESTAMP',
                            'ARRAYOFDOUBLES',
                            'ARRAYOFLONGS',
                            'ALARM'
                        ],
                        required=True)
    parser.add_argument(
                        '-v',
                        '--iasiovalue',
                        help = 'The value of the IASIO to push (for an alarm use the format ALARMSTATE:ALARMPRIORITY, e.g. SET_ACK:HIGH)',
                        action='store',
                        type=str,
                        required=True)
    parser.add_argument(
                        '-o',
                        '--opermode',
                        help = 'The operational mode of the IASIO to push',
                        action='store',
                        type=str,
                        choices=[
                            "STARTUP",
                            "INITIALIZATION",
                            "CLOSING",
                            "SHUTTEDDOWN",
                            "MAINTENANCE",
                            "OPERATIONAL",
                            "DEGRADED",
                            "UNKNOWN",
                            "MALFUNCTIONING"
                        ],
                        default = "OPERATIONAL",
                        required=False)
    parser.add_argument(
                        '-a',
                        '--validity',
                        help = 'The validity of the IASIO to push',
                        action='store',
                        type=str,
                        choices=[ "RELIABLE", "UNRELIABLE" ],
                        default="RELIABLE",
                        required=False)
    Log.add_log_arguments_to_parser(parser)
    return parser.parse_args()

def main() -> int:
    args = parse_args()
    Log.init_log_from_cmdline_args(args, __file__)
    global logger
    logger = logging.getLogger(__name__)

    # Build the Identifier to check if the provided frid is valid
    ias_identifier = Identifier.from_string(args.iasioid)
    
    bsdb_url = Utils.get_bsdb_url(args.kafkabrokers, args.jCdb)
    topic = IasKafkaHelper.topics['core']
    logger.debug(f"Creating Kafka producer with topic {topic} and BSDB URL {bsdb_url} and client ID {args.bsdbid}")
    producer = KafkaValueProducer(bsdb_url, topic, args.bsdbid)
    logger.info(f"Kafka IasValue producer created with BSDB URL {bsdb_url} and client ID {args.bsdbid}")

    ias_value = IasValue.build(
        value = args.iasiovalue,
        value_type = IASType.fromString(args.iasiotype),
        fr_id = ias_identifier,
        validity = Validity.fromString(args.validity),
        mode=OperationalMode.fromString(args.opermode))
    logger.info(f"IasValue to push: {ias_value.toJSonString()}")

    producer.send(ias_value)
    producer.flush()
    logger.info("IasValue sent to kafka topic %s", topic)
    return 0
    

if __name__ == '__main__':
    try:
        exit(main())
    except Exception as e:
        print(f"Error in main: {e}")
        exit(1)
        
