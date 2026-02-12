#! /usr/bin/env -S python3 -u
"""
ACK an alarm by sending a command to the Supervisor.

iasAckAlarm sends the command to ACK an alarm to the supervisor.
The script requires the full running ID of the alarm to acknowledge

If the frID, if not known, it can be taken from the alarms dumped 
in the stdout by iasLogDumper.

Created on Jun 12, 2024
"""

import argparse
import uuid
import os
import time

from IASLogging.logConf import Log
from IasCdb.CdbReader import CdbReader
from IasBasicTypes.Identifier import Identifier
from IasBasicTypes.IdentifierType import IdentifierType
from IasCmdReply.IasCommandSender import IasCommandSender
from IasExtras.AlarmAck import AlarmAck


# The logger, intialized by main()
logger = None

def parse_args():
    parser = argparse.ArgumentParser("ACK an alarm")
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
                        help='BSDB client ID used by Producer and Consumer (default iasAckAlarm-client-<UUID>)',
                        action='store',
                        default=f"iasAckAlarm-client-{uuid_id}",
                        required=False)
    parser.add_argument(
                        '-a',
                        '--alarmid',
                        help='The full running ID of the alarm to acknowledge',
                        action='store',
                        type=str,
                        required=True)
    parser.add_argument(
                        '-c',
                        '--comment',
                        help='The comment to add to the alarm',
                        action='store',
                        default = "No comment given",
                        type=str,
                        required=False)
    
    parser.add_argument(
                        '-t',
                        '--timeout',
                        help='The time to wait for the reply, 0 means do not wait (default 0 seconds)',
                        default=0,
                        type=float,
                        action='store',
                        required=False)
    parser.add_argument(
                        '-lfi',
                        '--levelFile',
                        help='Logging level: Set the level of the message for the file logger, default: Debug level',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='debug',
                        required=False)
    parser.add_argument(
                        '-lcon',
                        '--levelConsole',
                        help='Logging level: Set the level of the message for the console logger, default: Debug level',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)
    return parser.parse_args()

def get_bsdb_from_cdb(cdb_folder: str) -> str :
    """
    Get the BSDB URL from the CDB
    
    :param cdb_folder: The parend folder of the CDB
    :type cdb_folder: str
    :return: The URL to connect to the BSDB read from the IAS CDB
    :rtype: str
    """
    logger.debug("Getting BSDB URL from IAS CDB")
    cdb_reader = CdbReader(cdb_folder)
    ias_dao = cdb_reader.get_ias()
    return ias_dao.bsdb_url

def get_bsdb_url(kafka_brokers, jCdb) -> str :
    """
    Get the BSDB URL from (whatever is true)
    - the kafka brokers passed in the command line
    - the IAS CDB passed in the command line
    - the IAS_CDB from the environment variable, if defined
    - default
    
    :param kafka_brokers: kafka brokers URL from command line
    :param jCdb: Description CDB folder from the command line
    :return: The URL to connect to the BSDB
    :rtype: str
    """
    if kafka_brokers is not None:
        logger.debug("Using kafka brokers from command line")
        return kafka_brokers
    elif jCdb is not None:
        logger.debug("Using kafka brokers from IAS CDB passed in command line")
        return get_bsdb_from_cdb(jCdb)
    elif os.getenv("IAS_CDB") is not None:
        logger.debug("Using kafka brokers from IAS_CDB environment variable")
        # get the kafka brokers from the IAS_CDB env var, if defined
        return get_bsdb_from_cdb(os.environ["IAS_CDB"])
    else:
        return "localhost:9092"
    
def get_supervisor_id(alarm_frid: str) -> str:
    """
    Get the supervisor ID from the alarm full running ID

    :param alarm_frid: The full running ID of the alarm
    :type alarm_frid: str
    :return: The supervisor ID
    :rtype: str
    """
    alarm_id = Identifier.from_string(alarm_frid)
    return alarm_id.get_id_of_type(IdentifierType.SUPERVISOR)

def main()-> int:
    """
    Ack the alarm
    
    :return: 0 if successful, 1 otherwise
    :rtype: int
    """
    args = parse_args()
    global logger
    logger = Log.getLogger(__file__, fileLevel=args.levelFile, consoleLevel=args.levelConsole)
    bsdb_url = get_bsdb_url(args.kafkabrokers, args.jCdb)
    logger.info(f"BSDB URL: {bsdb_url}")

    supervisor_id = get_supervisor_id(args.alarmid)
    if not supervisor_id:
        raise ValueError(f"Cannot extract the Supervisor ID from the alarm ID {args.alarmid}")
    
    logger.info(f"Supervisor ID: {supervisor_id}")

    full_running_id="(iasAlarmAck:CLIENT)"

    cmd_sender = IasCommandSender(sender_full_running_id=full_running_id, bsdb_sender_id=bsdb_url, brokers=bsdb_url)
    cmd_sender.set_up()


    acker = AlarmAck(full_running_id=full_running_id, command_sender=cmd_sender)
    acker.start()

    acked = acker.ack(alarm_id=args.alarmid, 
                      supervisor_id=supervisor_id,
                      comment=args.comment,
                      timeout=args.timeout)
    acker.close()
    cmd_sender.close()
    logger.info("Alarm acknowledgment process completed.")
    return 0
    
                    
if __name__ == '__main__':
    try:
        exit(main())
    except Exception as e:
        print(f"Error in main: {e}")
        exit(1)