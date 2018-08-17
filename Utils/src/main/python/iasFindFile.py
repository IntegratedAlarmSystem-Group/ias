#! /usr/bin/env python
'''
Created on Sep 21, 2016

@author: acaproni
'''
import argparse
from IASTools.FileSupport import FileSupport
from IASLogging.logConf import Log
import os
if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='Search for a file in the hierarchy of IAS folders.')
    parser.add_argument(
        '-t',
        '--type',
        help='The type of files to search for: '+str(FileSupport.iasFileType),
        dest="fileType",
        action='store',
        default=None)
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
    parser.add_argument(
        dest='fileName',
        help='The name of the file to search for')
    args = parser.parse_args()
    stdoutLevel=args.levelStdOut
    consoleLevel=args.levelConsole
    logger = Log.initLogging(__file__,stdoutLevel,consoleLevel)
    fileName=args.fileName
    try:
        if not args.fileType is None:
            fileSupport = FileSupport(args.fileName, args.fileType)
            filePath=fileSupport.findFile()
        else:
            fileSupport = FileSupport(args.fileName)
            filePath=fileSupport.findFile()
        print(filePath)
    except FileNotFoundError as e:
        logger.error("File not found")
