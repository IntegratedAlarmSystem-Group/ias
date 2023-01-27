#! /usr/bin/env python3
'''
Created on Sep 26, 2016

Run a IAS tool like the supervisor.

@author: acaproni
'''

import argparse
import os
import socket
import sys
from subprocess import call

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
        log_file_name="ScalaTest"
    else:
        temp = className.rsplit(".", 1)
        log_file_name = temp[len(temp)-1]

    # Append the log identifier if it has been passed in the command line
    if len(logFileNameId.strip())>0:
        log_file_name = log_file_name+"-"+logFileNameId.strip()
    propsDict["ias.logs.filename"]= log_file_name

    propsDict["ias.config.folder"]=os.environ["IAS_CONFIG_FOLDER"]

    # Set the config file for sl4j (defined in Logging)
    logback_config_file_name="logback.xml"
    fs = FileSupport(logback_config_file_name,"config")
    try:
        path = fs.findFile()
        propsDict["logback.configurationFile"]=path
    except:
        logger.info("No log4j config file (%s) found: using defaults",logback_config_file_name)

    # JVM always uses UTC
    propsDict["user.timezone"]="UTC"

    # Add environment variables is not needed as environment variables
    # can be retrieved with System.getEnv


def addUserProps(propsDict, userProps):
    """
    Adds to the dictionary the user properties passed in the command line.

    @param propsDict: A dictionary of properties in the form name:value
    @param userProps: a list of java properties in the form name=value
    """
    if userProps is None or len(userProps)==0:
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

    # The parser does not provide the help because teh default help
    # shows the message and terminates the application.
    # The user is instead more probably interested in the help of the tool executed by iasRun
    parser = argparse.ArgumentParser(description='Run a IAS java or scala program.', add_help=False)
    parser.add_argument(
                        '-i',
                        '--logfileId',
                        help='The identifier to be appended to the name of the log file',
                        action='store',
                        default="",
                        required=False)
    parser.add_argument(
        '-c',
        '--className',
        help='The scala or java class to execute',
        action='store',
        required=True)
    parser.add_argument(
                        '-D',
                        '--jProp',
                        help='Set a java property: -Dname=value',
                        nargs="*",
                        action='append',
                        required=False)
    parser.add_argument(
        '-h',
        '--help',
        help='Print this help and the help of the scala/java program',
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
                        '-lf',
                        '--logLevelFile',
                        help='Logging level: Set the level of the message for the file logger of iasRun (default: info)',
                        action='store',
                        choices=['info', 'debug', 'warn', 'error', 'critical'],
                        default='info',
                        required=False)
    parser.add_argument(
                        '-lc',
                        '--logLevelConsole',
                        help='Logging level: Set the level of the message for the console logger of iasRun, (default: info)',
                        action='store',
                        choices=['info', 'debug', 'warn', 'error', 'critical'],
                        default='info',
                        required=False)

    parser.add_argument('params', nargs=argparse.REMAINDER, help='Command line parameters to pass to the JVM executable')
    args = parser.parse_args()

    print("Parsed ARGS:", args)

    if args.help:
        parser.print_help()
        print("Triggering the help of the java/scala executable...")

    # Start the logger with param define by the user.
    stdoutLevel = args.logLevelFile
    consoleLevel = args.logLevelConsole
    logger = Log.getLogger(__file__, stdoutLevel, consoleLevel)

    # Is the environment ok?
    # Fail fast!
    if not CommonDefs.checkEnvironment():
        logger.info("Some setting missing in IAS environment.")
        logger.info("Set the environment with ias-bash_profile before running IAS applications")
        sys.exit(-1)

    logger.info("Start IASRun")

    # Get java options from JAVA_OPTS environment variable
    javaOptions = javaOpts()

    # Build the command line in cmd
    cmd = ['java']

    # Enable/Disable assertions
    enableAssertions = args.assertions
    if enableAssertions:
        javaOptions.append("-ea")
        logger.debug("Assertions are enabled.")
    else:
        logger.debug("Assertions disabled.")

    # Actual environment to pass to the executable
    #
    # It is enriched by setting JAVA_OPTS for scala
    d = dict(os.environ)

    # Add java options
    for opt in javaOptions:
        logger.debug("Adding %s java option", opt)
        cmd.append(opt)

    # Create tmp and logs folders if not exists already
    FileSupport.createLogsFolder()
    FileSupport.createTmpFolder()

    # Add the properties
    #
    # Default and user defined properties are in a dictionary:
    # this way it is easy for the user to override default properties.
    props = {}
    setProps(props, args.className, args.logfileId)
    if args.jProp is not None:
        addUserProps(props, args.jProp)
    if len(props) > 0:
        stingOfPros = formatProps(props)
        # Sort to enhance readability
        stingOfPros.sort()
        cmd.extend(formatProps(props))
        logger.debug("java properties:")
        for p in stingOfPros:
            logger.debug("\t %s", p[2:])
    else:
        logger.debug("No java properties defined")

    # Add the classpath
    theClasspath = CommonDefs.buildClasspath()
    cmd.append("-cp")
    cmd.append(theClasspath)

    jars = theClasspath.split(":")
    jars.sort()
    logger.debug("Classpath:")
    for jar in jars:
        logger.debug("\t %s",jar)

    # Add the class
    cmd.append(args.className)

    # Finally the command line parameters
    if args.help:
        # Forward the help
        cmd.append("-h")
    if len(args.params)>0:
        cmd.extend(args.params)

    if len(args.params) > 0:
        logger.debug("with params:")
        for arg in args.params:
            logger.debug("\t %s", arg)
    else:
        logger.debug("with no params")

    arrowDown =  chr(8595)
    delimiter = ""
    for t in range(16):
        delimiter = delimiter + arrowDown

    logger.debug("Will run %s", cmd)
    logger.info("%s %s output %s", delimiter, args.className, delimiter)

    call(cmd)

    arrowUp =  chr(8593)
    delimiter = ""
    for t in range(17):
        delimiter = delimiter + arrowUp
    logger.info("%s %s done %s", delimiter, args.className, delimiter)
