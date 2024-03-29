#! /usr/bin/env python3
'''
Writes the classpath in the stdout

@author: acaproni
'''
import argparse
import os

from IASLogging.logConf import Log
from IASTools.CommonDefs import CommonDefs

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Get the classpath.')
    parser.add_argument(
                        '-lso',
                        '--levelStdOut',
                        help='Logging level: Set the level of the message for the file logger, default: Debug level',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)
    parser.add_argument(
                        '-lcon',
                        '--levelConsole',
                        help='Logging level: Set the level of the message for the console logger, default: Debug level',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)
    args = parser.parse_args()
    stdoutLevel=args.levelStdOut
    consoleLevel=args.levelConsole
    logger=Log.getLogger(os.path.basename(__file__),stdoutLevel,consoleLevel)
    print(CommonDefs.buildClasspath())
