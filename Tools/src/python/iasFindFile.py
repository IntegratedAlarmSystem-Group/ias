#! /usr/bin/env python
'''
Created on Sep 21, 2016

@author: acaproni
'''
import argparse
from IASTools.FileSupport import FileSupport
from logConf import Log

if __name__ == '__main__':
    #import Logger from logConf
    logger = Log.GetLogger(os.path.basename(__file__).split(".")[0])

    parser = argparse.ArgumentParser(description='Search for a file in the hierarchy of IAS folders.')
    parser.add_argument(
        '-t',
        '--type',
        help='The type of files to search for: '+str(FileSupport.iasFileType),
        dest="fileType",
        action='store',
        default=None)
    parser.add_argument(
        dest='fileName',
        help='The name of the file to search for')
    args = parser.parse_args()
    logger.info("Search for a file in the hierarchy of IAS folders")
    try:
        if not args.fileType is None:
            fileSupport = FileSupport(args.fileName, args.fileType)
            filePath=fileSupport.findFile()
        else:
            fileSupport = FileSupport(args.fileName)
            filePath=fileSupport.findFile()
        logger.warning("File found in the path %s",filePath)
        print(filePath)
    except OSError as e:
        print("File not found")
        logger.warning("File not found")
