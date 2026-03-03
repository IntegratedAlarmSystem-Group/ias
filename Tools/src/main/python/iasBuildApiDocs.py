#! /usr/bin/env python3
'''
Build API documentation for java, scal and python
Created on Jul 7, 2017

@author: acaproni
'''
import logging
from argparse import ArgumentParser
from os.path import join

from IASApiDocs.JavadocBuilder import JavadocBuilder
from IASApiDocs.PydocBuilder import PydocBuilder
from IASApiDocs.ScaladocBuilder import ScaladocBuilder
from IASLogging.log import Log

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
    Log.add_log_arguments_to_parser(parser)
    
    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, __file__)
    
    logger=logging.getLogger(__name__)
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
