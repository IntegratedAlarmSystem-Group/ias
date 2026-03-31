#! /usr/bin/env python3
'''
Created on Aug 18, 2016

@author: acaproni
'''

import argparse
import logging

from IasLogging.log import Log
from IasTools.ModuleSupport import ModuleSupport

if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='Creates a module for the Integrated Alarm System.')
    parser.add_argument(
                        '-e',
                        '--erase',
                        help='Erase the module if it already exists',
                        action='store_true',
                        default=False)
    parser.add_argument('moduleName', help='The name of the IAS module to create')
    Log.add_log_arguments_to_parser(parser)
    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, __file__)
    logger=logging.getLogger(__name__)
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
