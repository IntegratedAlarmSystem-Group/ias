#! /usr/bin/env python3
'''
Writes the classpath in the stdout

@author: acaproni
'''
import argparse

from IasLogging.log import Log
from IasTools.CommonDefs import CommonDefs

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Get the classpath.')
    Log.add_log_arguments_to_parser(parser)
    
    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, name_file=__file__) 
    print(CommonDefs.buildClasspath())
