'''
Created on Sep 22, 2016

@author: acaproni
'''

import logging
from os import environ, walk, path

from . import FileSupport


class CommonDefs(object):
    """
    A collection of useful methods.
    Some of them could probably be moved somewhere else...
    """
    
    # Classpath separator for jars
    __classPathSeparator = ":"

    # The logger
    logger = logging.getLogger("CommonDefs")
    
    @classmethod
    def buildClasspath(cls):
        """
        Build the class path by reading the jars from the
        IAS hierarchy of folders and external jar from the
        folder pointed by the IAS-EXTERNAL-JARS environment variable

        @return: A string with the jars in the classpath
        """

        # jars list is used to avoid duplications of jars in the classpath
        # It contains all the jars without the path
        # i.e. lc.jar but not ../lib/lc.jar
        jars = []

        # The classpath
        classpath = ""

        # Get the folder with external jars from the environment variable, if exists
        externalJarsPath = None
        try:
           externalJarsPath = environ['IAS_EXTERNAL_JARS']
        except:
            pass

        # Adds the jar files in the external folder
        if externalJarsPath is not None:
            CommonDefs.logger.info("Defined a folder for external jars: %s",externalJarsPath)
            # Check if externalJarsPath is a directory and is readable
            if not path.isdir(externalJarsPath):
                CommonDefs.logger.error("Unreadable folder of external jars: %s",externalJarsPath)
            else:
                for root, subFolders, files in walk(externalJarsPath):
                    for jarFileName in files:
                        if (jarFileName.lower().endswith('.jar') and jars.count(jarFileName)==0):
                            filePath=path.join(root,jarFileName)
                            if classpath:
                                classpath=classpath+cls.__classPathSeparator
                            classpath=classpath+filePath
                            jars.append(jarFileName)
        else:
            CommonDefs.logger.info("No folder for external JARs defined (i.e. no IAS-EXTERNAL-JARS env. variable set)")

        # Add the jars from the current module and IAS_ROOT lib folders
        for folder in FileSupport.FileSupport.getIASFolders('lib'):
            for root, subFolders, files in walk(folder):
                for jarFileName in files:
                    if (jarFileName.lower().endswith('.jar') and jars.count(jarFileName)==0):
                        filePath=path.join(root,jarFileName)
                        if classpath:
                            classpath=classpath+cls.__classPathSeparator
                        classpath=classpath+filePath
                        jars.append(jarFileName)
        return classpath
                            
    @classmethod
    def checkEnvironment(cls):
        """
        Check if IAS enviroment is correctly set up
        
        @return: True is the enviroment is correctly set;
                 False otherwise
        """
        try:
            environ["JAVA_HOME"]
            environ["JRE_HOME"]
            environ["SCALA_HOME"]
            environ["PYTHONPATH"]
            environ["IAS_ROOT"]
            environ["IAS_LOGS_FOLDER"]
            return True
        except:
            return False
    
    @classmethod
    def addScalaJarsToClassPath(cls,classpath):
        """
        Append scala jars to the passed classpath
        
        scala jars are needed by java programs that calls scala code
        
        @param classpath: the classpath to add scala jars to
        """
        scalaLibFolder=  path.join(environ["SCALA_HOME"],"lib")  
        for root, subFolders, files in walk(scalaLibFolder):
            for jarFileName in files:
                if (jarFileName.lower().endswith('.jar')):
                    filePath=path.join(root,jarFileName)
                    classpath=classpath+cls.__classPathSeparator
                    classpath=classpath+filePath
        return classpath   
