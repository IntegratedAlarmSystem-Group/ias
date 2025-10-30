#! /usr/bin/env python3

'''
Send a command through the command kafka topic.

This script pushes a command in the topic and exits immediately.
It does not wait for the reception of the reply: if interested in the reception of a reply,
use a command like
> iasDumpKafkaTopic -t reply|jq
If it is the case, grep with the ID of the sender and/or the receiver to limit the
number of replies printed by the command.

LIMITATIONS:
  - the command does not support properties
  - the default identifier of the kafka broker is composed by the command, the name of the user and the host
    so it does not ensure 100% to be unique as requested by kafka: a parameter in the command line
    allows to pass a user defined identifier
'''

import argparse
import getpass
import os
import socket

import sys

from IASLogging.logConf import Log
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasCommandSender import IasCommandSender


def on_send_error(excp):
    logger.error('Error pushing command in the kafka topic', exc_info=excp)

if __name__ == '__main__':

    userName = getpass.getuser()
    hostName = socket.gethostname()

    commands = []
    for cmd in IasCommandType:
        commands.append(cmd.name)

    log_levels = [ 'DEBUG', 'INFO' , 'WARNING', 'ERROR', 'CRITICAL']

    temp = sys.argv[0].split(os.path.sep)
    progName = temp[len(temp)-1]
    defaultId = "%s-by-%s-at-%s" % (progName,userName,hostName)

    parser = argparse.ArgumentParser(description='Send commands though the command topic', prog=progName)
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
        '-d',
        '--dest',
        help='The ID of the destination of the command',
        action='store',
        type=str,
        required=True)

    parser.add_argument(
        '-s',
        '--sender',
        help='The ID of the sender of the command',
        action='store',
        type=str,
        default=defaultId,
        required=False)

    parser.add_argument(
        '-c',
        '--command',
        help='The command to be executed at the destination',
        action='store',
        choices=commands,
        type=str,
        required=True)



    
    parser.add_argument(
        '-lso',
        '--levelStdOut',
        help='Logging level: Set the level of the message for the file logger, default: Info level',
        action='store',
        choices=['info', 'debug', 'warning', 'error', 'critical'],
        default='info',
        required=False)
    parser.add_argument(
        '-lcon',
        '--levelConsole',
        help='Logging level: Set the level of the message for the console logger, default: Info level',
        action='store',
        choices=['info', 'debug', 'warning', 'error', 'critical'],
        default='warning',
        required=False)

    parser.add_argument('params', nargs='*', help="Command parameters")

    args = parser.parse_args()

    stdoutLevel=args.levelStdOut
    consoleLevel=args.levelConsole
    logger = Log.getLogger(__file__, stdoutLevel, consoleLevel)

    if not args.params:
        logger.info("Going to send command %s to %s", args.command, args.dest)
    else:
        logger.info("Going to send command %s to %s and params %s",args.command,args.dest,str(args.params))

    senderFullRunningId = "(%s:CLIENT)" % (args.sender)
    commandType = IasCommandType.fromString(args.command)

    if not args.params:
        params = None
    else:
        params = args.params

    # Check if the command line contains all and only the requested parameters
    if commandType.num_of_params==0 and params is not None:
        logger.error("Command %s rejected: it takes no parameters but got %s",args.command,str(params))
        sys.exit(-1)
    elif commandType.num_of_params>0  and (params is None or commandType.num_of_params!=len(params)):
            logger.error("Command %s rejected: it takes %d parameters but got %s",args.command,commandType.num_of_params,str(params))
            sys.exit(-1)

    kafkaBrokers = "%s:%d" % (args.broker, args.port)

    sender_id = "iasSendCmd_"+args.sender

    cmd_sender  = IasCommandSender(senderFullRunningId, sender_id, kafkaBrokers)
    cmd_sender.set_up()

    # Send the command synchronously to be sure that it is effectively sent
    # before the script terminates
    cmd_sender.send_sync(
        args.dest,
        IasCommandType.fromString(args.command),
        args.params)

    logger.info("Command sent.")

    cmd_sender.close()
    logger.info("Done")
