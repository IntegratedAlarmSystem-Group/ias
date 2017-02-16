#!/usr/bin/python
'''
Created on Sep 26, 2016

@author: acaproni
'''

import argparse
import cmd
import os
import sys
from IASTools.CommonDefs import CommonDefs
from subprocess import call
from IASTools.FileSupport import FileSupport


def setProps(propsDict):
    """
    Adds to the passed dictionary, the properties to be passed to all java/scala
    executables.
    
    These properties are common to all IAS executables and can for example 
    be configuration properties.
    
    @param propsDict: A dictionary of properties in the form name:value
    """
    # Environment variables
    propsDict["ias.root.folder"]=os.environ["IAS_ROOT"]
    propsDict["ias.logs.folder"]=os.environ["IAS_LOGS_FOLDER"]
    propsDict["ias.config.folder"]=os.environ["IAS_CONFIG_FOLDER"]
    
    # Set the config file for sl4j (defined in Logging)
    logbackConfigFileName="logback.xml"
    fs = FileSupport(logbackConfigFileName,"config")
    try:
        path = fs.findFile()
        propsDict["logback.configurationFile"]=path
    except:
        print "No log4j config file ("+logbackConfigFileName+") found: using defaults"
    
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
            print "Invalid property:",prop[0]
            print "Expected format is name=value"
            print
            sys.exit(-1)
        # Is th eprop. already defined?
        if parts[0] in propsDict:
            print "AWARNING: overriding ",parts[0],"java property"
            print "\told value",propsDict[parts[0]],"new value",parts[1]
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
    
    keys = propsDict.keys()
    for key in keys:
        propStr = "-D"+key+"="+propsDict[key]
        ret.append(propStr)
    return ret

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
                        '-D',
                        '--jProp',
                        help='Set a java property: -Dname=value',
                        nargs="+",
                        action='append',
                        required=False)
    parser.add_argument('className', help='The name of the class to run the program')
    parser.add_argument('params', metavar='param', nargs='*',
                    help='Command line parameters')
    args = parser.parse_args()
    
    # Build the command line
    if args.language=='s' or args.language=='scala':
        cmd=['scala']
    else:
        cmd=['java']
        
    # Is the environment ok?
    # Fail fast!
    if not CommonDefs.checkEnvironment():
        print "Some setting missing in IAS environment."
        print "Set the environment with ias-bash_profile before running IAS applications"
        print
        sys.exit(-1)
    
    # Create logs folder if not exists already
    FileSupport.createLogsFolder()
    
    # Add the properties
    #
    # Default and user defined properties are in a dictionary:
    # this way it is easy for the user to overrride default properties.
    props={}
    setProps(props)
    if args.jProp is not None:
        addUserProps(props,args.jProp)
    if len(props)>0:
        stingOfPros = formatProps(props)
        # Sort to enhance readability
        stingOfPros.sort()
        cmd.extend(formatProps(props))
    
    
    #add the classpath
    cmd.append("-cp")
    cmd.append(CommonDefs.buildClasspath())
    
    # Add the class
    cmd.append(args.className)
    
    # Finally the command line parameters
    if len(args.params)>0:
        cmd.extend(args.params)
    
    print
    print "Going to run:"
    print cmd
    print
        
    call(cmd)
    
    