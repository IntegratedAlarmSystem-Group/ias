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
import logging

from IASLogging.log import Log
from IasBasicTypes.Identifier import Identifier
from IasBasicTypes.IdentifierType import IdentifierType
from IasCmdReply.IasCommandSender import IasCommandSender

from IasExtras.AlarmAck import AlarmAck
from IasExtras.Utils import Utils


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
    Log.add_log_arguments_to_parser(parser)
    return parser.parse_args()
    
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
    Log.init_log_from_cmdline_args(args, __file__)
    global logger
    logger = logging.getLogger(__name__)
    bsdb_url = Utils.get_bsdb_url(args.kafkabrokers, args.jCdb)
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

    try:
        acked = acker.ack(alarm_id=args.alarmid, 
                        supervisor_id=supervisor_id,
                        comment=args.comment,
                        timeout=args.timeout)
    except:
        logger.exception("Error while acknowledging the alarm")
        acked = False
    finally:
        logger.debug("Closing")
        acker.close()
        cmd_sender.close()
    return 0 if acked else 1
    
                    
if __name__ == '__main__':
    try:
        exit(main())
    except Exception as e:
        print(f"Error in main: {e}")
        exit(1)