#! /usr/bin/env python3
'''
Created on Sep 21, 2016

@author: acaproni
'''
import argparse
import logging

from IasLogging.log import Log
from IasTools.FileSupport import FileSupport

if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='Search for a file in the hierarchy of IAS folders.')
    parser.add_argument(
        '-t',
        '--type',
        help='The type of files to search for: (lin, bin, config, src)',
        dest="fileType",
        action='store',
        default=None)
    Log.add_log_arguments_to_parser(parser)
    parser.add_argument(
        dest='fileName',
        help='The name of the file to search for')
    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, __file__)
    logger = logging.getLogger("iasFindFile")
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
