#! /usr/bin/env python
'''
Created on Aug 18, 2016

@author: acaproni
'''

import argparse

from IASTools.ModuleSupport import ModuleSupport
from IASLogging.logConf import Log

if __name__ == '__main__':
    log=Log()
    logger=log.GetLoggerFile()
    parser = argparse.ArgumentParser(description='Creates a module for the Integrated Alarm System.')
    parser.add_argument(
                        '-e',
                        '--erase',
                        help='Erase the module if it already exists',
                        action='store_true',
                        default=False)
    parser.add_argument('moduleName', help='The name of the IAS module to create')
    args = parser.parse_args()
    
    if args.erase:
        try:
            ModuleSupport.removeExistingModule(args.moduleName)
        except Exception as e:
            logger.error("Error deleting the module: %s",str(e))
            exit(-1)
    try:
        ModuleSupport.createModule(args.moduleName)
    except Exception as e:
        logger.error("Error creating the module: %s",str(e))
        exit(-1)
    
