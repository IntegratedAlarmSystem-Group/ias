#! /usr/bin/env python3
'''
Build API documentation for java, scal and python
Created on Jul 7, 2017

@author: acaproni
'''

from argparse import ArgumentParser
from os.path import join

from IASApiDocs.JavadocBuilder import JavadocBuilder
from IASApiDocs.PydocBuilder import PydocBuilder
from IASApiDocs.ScaladocBuilder import ScaladocBuilder
from IASLogging.logConf import Log

if __name__ == '__main__':

    # Parse the command line
    parser = ArgumentParser()
    parser.add_argument(
        "-d", 
        "--destFolder", 
        help="HTML destination folder", 
        action="store",
        dest="destFolder",
        required=True)
    parser.add_argument(
        "-s", 
        "--sourceFolder", 
        help="IAS source folder", 
        action="store", 
        dest="srcFolder",
        required=True)
    parser.add_argument(
        "-l",
        "--language",
        help="The language to generate the documentation for (default all)",
        action='store',
        choices=['all', 'scala', 'java', 'python'],
        default='all',
        required=False
    )
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
    print(args)
    
    stdoutLevel=args.levelStdOut
    consoleLevel=args.levelConsole
    logger=Log.getLogger(__file__,stdoutLevel,consoleLevel)
    logger.info("Reading sources from %s",args.srcFolder)

    # Build scala documentation
    if args.language=='all' or args.language=='scala':
        logger.info("Building scaladoc")
        scalaBuilder = ScaladocBuilder(args.srcFolder,join(args.destFolder,"scala"))
        scalaBuilder.buildScaladocs()

    if args.language=='all' or args.language=='java':
        logger.info("Building javadoc")
        javaBuilder = JavadocBuilder(args.srcFolder,join(args.destFolder,"java"))
        javaBuilder.buildJavadocs()

    if args.language=='all' or args.language=='python':
        logger.info("Building pydoc")
        pythonBuilder = PydocBuilder(args.srcFolder,join(args.destFolder,"python"))
        pythonBuilder.buildPydocs()
