#! /usr/bin/env python
'''
Created on Sep 26, 2016

@author: acaproni
'''

import argparse
import os
import socket
from subprocess import call

import sys

from IASLogging.logConf import Log
from IASTools.CommonDefs import CommonDefs
from IASTools.FileSupport import FileSupport


def setProps(propsDict,className,logFileNameId):
    """
    Adds to the passed dictionary, the properties to be passed to all java/scala
    executables.

    These properties are common to all IAS executables and can for example
    be configuration properties.

    @param propsDict: A dictionary of properties in the form name:value
    @param className The name of the class to run
    @param logFileNameId A string identifier to append to the name of the log file
    """
    # Environment variables
    propsDict["ias.root.folder"]=os.environ["IAS_ROOT"]
    propsDict["ias.logs.folder"]=os.environ["IAS_LOGS_FOLDER"]
    propsDict["ias.tmp.folder"]=os.environ["IAS_TMP_FOLDER"]

    # The name of the host where the tool runs
    propsDict["ias.hostname"]=socket.gethostname()

    # get the name of the class without dots
    # i.e. if className = "org.eso.ias.supervisor.Supervisor" we want
    # the file to be named "Supervisor"
    if "scalatest" in className:
        # logs generated by scalatest are named "org.scalatest.run-2018-03-05T15-55-21.log"
        # so we catch this case here to have a more meaningful name then just "run"
        logFileName="ScalaTest"
    else:
        temp = className.rsplit(".",1)
        logFileName = temp[len(temp)-1]

    # Append the log identifier if it has been passed in the command line
    if len(logFileNameId.strip())>0:
        logFileName = logFileName+"-"+logFileNameId.strip()
    propsDict["ias.logs.filename"]= logFileName

    propsDict["ias.config.folder"]=os.environ["IAS_CONFIG_FOLDER"]

    # Set the config file for sl4j (defined in Logging)
    logbackConfigFileName="logback.xml"
    fs = FileSupport(logbackConfigFileName,"config")
    try:
        path = fs.findFile()
        propsDict["logback.configurationFile"]=path
    except:
        logger.info("No log4j config file (%s) found: using defaults",logbackConfigFileName)

    # JVM always uses UTC
    propsDict["user.timezone"]="UTC"

    # Add environment variables

def addUserProps(propsDict,userProps):
    """
    Adds to the dictionary the user properties passed in the command line.

    @param propsDict: A dictionary of properties in the form name:value
    @param userProps: a list of java properties in the form name=value
    """
    if userProps==None or len(userProps)==0:
        return
    for prop in userProps:
        if len(prop)==0:
            continue
        parts=prop[0].split('=')
        # Integrity check
        if len(parts)!=2:
            logger.info("Invalid property: %s",prop[0])
            logger.info("Expected format is name=value")
            sys.exit(-1)
        # Is th eprop. already defined?
        if parts[0] in propsDict:
            logger.info("\nWARNING: overriding %s java property",parts[0])
            logger.info("\told value %s new value %s",parts[1],propsDict[parts[0]])

        propsDict[parts[0]]=parts[1]

def formatProps(propsDict):
    """
    Format the dictionary of properties in a list of strings
    list [ "-Dp1=v1", -Dp2=v2"]

    @param propsDict: A dictionary of properties in the form name:value
    @return A list of java/scala properties
    """
    if len(propsDict)==0:
        return []

    ret = []

    keys = list(propsDict.keys())
    for key in keys:
        propStr = "-D"+key+"="+propsDict[key]
        ret.append(propStr)
    return ret

def javaOpts():
    """
    Handling of JAVA_OPTS environment variable to pass options to java executables differs
    from scala and java:
    * scala passes options to the java executable setting the JAVA_OPTS environment variable
    * java does not recognize JAVA_OPTS but accepts options as parameters in the command line

    To unify the handling of java option for the 2 programming languages,
    this method returns a list of java options (string).
    Depending on the executable they will be passed differently.

    @return: A list of java options like ['-Xmx512M', '-Xms16M']
    """
    try:
        javaOpts=os.environ["JAVA_OPTS"]
        return javaOpts.split()
    except:
        # JAVA_OPTS environment variable not defined
        return []

if __name__ == '__main__':
    """
    Run a java or scala tool.
    """

    parser = argparse.ArgumentParser(description='Run a java or scala program.')
    parser.add_argument(
                        '-l',
                        '--language',
                        help='The programming language: one between scala (or shortly s) and java (or j)',
                        action='store',
                        choices=['java', 'j', 'scala','s'],
                        required=True)
    parser.add_argument(
                        '-i',
                        '--logfileId',
                        help='The identifier to be appended to the name of the log file',
                        action='store',
                        default="",
                        required=False)
    parser.add_argument(
                        '-D',
                        '--jProp',
                        help='Set a java property: -Dname=value',
                        nargs="*",
                        action='append',
                        required=False)
    parser.add_argument(
                        '-v',
                        '--verbose',
                        help='Increase the verbosity of the output',
                        action='store_true',
                        default=False,
                        required=False)
    parser.add_argument(
                        '-a',
                        '--assertions',
                        help='Disable assertions',
                        action='store_false',
                        default=True,
                        required=False)
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

    parser.add_argument('className', help='The name of the class to run the program')
    parser.add_argument('params', nargs=argparse.REMAINDER,
                    help='Command line parameters')
    args = parser.parse_args()

    #Start the logger with param define by the user.
    stdoutLevel=args.levelStdOut
    consoleLevel=args.levelConsole
    logger = Log.initLogging(__file__,stdoutLevel,consoleLevel)

    logger.info("Start IASRun")
    verbose = args.verbose
    if verbose:
        logger.info("\nVerbose mode ON")

    # Get java options from JAVA_OPTS environment variable
    javaOptions=javaOpts()

    # Build the command line
    if args.language=='s' or args.language=='scala':
        cmd=['scala']
        if verbose:
            logger.info("Running a SCALA program.")
    else:
        cmd=['java']
        if verbose:
            logger.info("Running a JAVA program.")

    # Assertions are enabled differently in java (command line) and scala ($JAVA_OPTS)
    enableAssertions = args.assertions
    if enableAssertions:
        javaOptions.append("-ea")
        if verbose:
            logger.info("Assertions are enabled.")
    else:
        if verbose:
            logger.info("Assertions disabled.")


    # Actual environment to pass to the executable
    #
    # It is enriched by setting JAVA_OPTS for scala
    d = dict(os.environ)

    # Add java options (in the command line for java executables and
    # in JAVA_OPTS env. variable for scala)
    if args.language=='s' or args.language=='scala':
        s=" ".join(map(str, javaOptions))
        if verbose:
            logger.info("Options to pass to the java executable %s",s)
            d['JAVA_OPTS']=s
    else:
        for opt in javaOptions:
            if verbose:
                logger.info("Adding %s java option",opt)
            cmd.append(opt)

    # Is the environment ok?
    # Fail fast!
    if not CommonDefs.checkEnvironment():
        logger.info("Some setting missing in IAS environment.")
        logger.info("Set the environment with ias-bash_profile before running IAS applications")
        sys.exit(-1)

    # Create tmp and logs folders if not exists already
    FileSupport.createLogsFolder()
    FileSupport.createTmpFolder()

    # Add the properties
    #
    # Default and user defined properties are in a dictionary:
    # this way it is easy for the user to overrride default properties.
    props={}
    setProps(props, args.className,args.logfileId)
    if args.jProp is not None:
        addUserProps(props,args.jProp)
    if len(props)>0:
        stingOfPros = formatProps(props)
        # Sort to enhance readability
        stingOfPros.sort()
        cmd.extend(formatProps(props))
        if verbose:
            logger.info("java properties:")
            for p in stingOfPros:
                logger.info("\t %s",p[2:])
    else:
        if (verbose):
            logger.info("No java properties defined")


    #add the classpath
    theClasspath=CommonDefs.buildClasspath()
    if (args.language=='j' or args.language=='java'):
        theClasspath=CommonDefs.addScalaJarsToClassPath(theClasspath)
    cmd.append("-cp")
    cmd.append(theClasspath)
    if verbose:
        jars = theClasspath.split(":")
        jars.sort()
        logger.info("Classpath:")
        for jar in jars:
            logger.info("\t %s",jar)

    # Add the class
    cmd.append(args.className)

    # Finally the command line parameters
    if len(args.params)>0:
        cmd.extend(args.params)

    if verbose:

        if len(args.params)>0:
            logger.info("with params:")
            for arg in args.params:
                logger.info("\t %s",arg)
        else:
            logger.info()

    if verbose:
        arrowDown =  chr(8595)
        delimiter = ""
        for t in range(16):
            delimiter = delimiter + arrowDown

        logger.info("\n %s %s output %s",delimiter,args.className,delimiter)

    call(cmd)

    if verbose:
        arrowUp =  chr(8593)
        delimiter = ""
        for t in range(17):
            delimiter = delimiter + arrowUp
        logger.info("%s %s done %s",delimiter,args.className,delimiter)
