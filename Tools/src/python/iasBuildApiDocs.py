#! /usr/bin/env python
'''
Build API documentation for java, scal and python
Created on Jul 7, 2017

@author: acaproni
'''

import sys
from optparse import OptionParser
from IASApiDocs.ScaladocBuilder import ScaladocBuilder
from IASApiDocs.JavadocBuilder import JavadocBuilder
from IASApiDocs.PydocBuilder import PydocBuilder
from os.path import join
from logConf import Log

if __name__ == '__main__':
    # Parse the command line
    log=Log()
    fileName=os.path.basename(__file__).split(".")[0]
    logger=log.GetLoggerFile(fileName)
    parser = OptionParser()
    parser.add_option("-d", "--destFolder", help="HTML destination folder", action="store", type="string", dest="destFolder")
    parser.add_option("-s", "--sourceFolder", help="IAS source folder", action="store", type="string", dest="srcFolder")
    (options, args) = parser.parse_args()

    if not options.destFolder:
        logger.warning("No destrination folder given")
        sys.exit(-1)
    else:
        logger.info("API documentation will be generated in %s", options.destFolder)

    if not options.srcFolder:
        logger.warning("No source folder given")
        sys.exit(-1)
    else:
        logger.info("Reading sources from %s",options.srcFolder )

    # Build scala documentation
    logger.info("Building scaladoc")
    scalaBuilder = ScaladocBuilder(options.srcFolder,join(options.destFolder,"scala"))
    scalaBuilder.buildScaladocs()

    logger.info("Building javadoc")
    javaBuilder = JavadocBuilder(options.srcFolder,join(options.destFolder,"java"))
    javaBuilder.buildJavadocs()

    logger.info("Building pydoc")
    pythonBuilder = PydocBuilder(options.srcFolder,join(options.destFolder,"python"))
    pythonBuilder.buildPydocs()

