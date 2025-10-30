'''
Created on Sep 22, 2016

@author: acaproni
'''
from os import environ, walk, path, listdir

from . import FileSupport


class CommonDefs(object):
    """
    A collection of useful methods.
    Some of them could probably be moved somewhere else...
    """
    
    # Classpath separator for jars
    __classPathSeparator = ":"


    @classmethod
    def buildClasspath(cls):
        """
        Build the class path by reading the jars from the
        IAS hierarchy of folders and external jar from the
        folder pointed by the IAS-EXTERNAL-JARS environment variable

        @return: A string with the jars in the classpath
        """
        from IASLogging.logConf import Log
        _logger = Log.getLogger(__file__)
        
        # jars list is used to avoid duplications of jars in the classpath
        # It contains all the jars without the path
        # i.e. lc.jar but not ../lib/lc.jar
        jars = []

        # The classpath
        classpath = ""

        # Get the folders with external jars from the environment variable, if exists
        externalJarsPaths = []
        try:
           paths = environ['IAS_EXTERNAL_JARS'].split(":")
           for folder in paths:
               if path.exists(folder) and path.isdir(folder):
                   externalJarsPaths.append(folder)
                   _logger.debug("Folder %s added to the external folders of jars",folder)
               else:
                   _logger.warn("Folder of jars %s discarded", folder)
        except:
            _logger.debug("No external jars defined")

        # Adds the jar files in the external folders
        if externalJarsPaths:
            _logger.info("Found %d folders of external jars", len(externalJarsPaths))
            for folder in externalJarsPaths:
                for root, subFolders, files in walk(folder):
                    for jarFileName in files:
                        if (jarFileName.lower().endswith('.jar') and jars.count(jarFileName)==0):
                            filePath=path.join(root,jarFileName)
                            if classpath:
                                classpath=classpath+cls.__classPathSeparator
                            classpath=classpath+filePath
                            jars.append(jarFileName)
        else:
            _logger.info("No folders of external JARs defined (i.e. no IAS-EXTERNAL-JARS env. variable set)")

        # Add the jars from the current module and IAS_ROOT lib folders
        for folder in FileSupport.FileSupport.getIASFolders('lib'):
            for root, subFolders, files in walk(folder):
                for jarFileName in files:
                    if (jarFileName.lower().endswith('.jar') and jars.count(jarFileName)==0):
                        filePath = path.join(root,jarFileName)
                        if classpath:
                            classpath = classpath+cls.__classPathSeparator
                        classpath = classpath+filePath
                        jars.append(jarFileName)

        # Adds test classes, if any
        testFolders = FileSupport.FileSupport.getClassFolders()
        for folder in testFolders:
            classpath = classpath+cls.__classPathSeparator+folder

        # Adds the test resources folder to the classpath, if exists and not empty
        resFolder = "src/test/resources"
        if path.isdir(resFolder) and listdir(resFolder):
            classpath = classpath+cls.__classPathSeparator+resFolder
            _logger.info("Directory is not empty")

        return classpath
