'''
Created on Sep 22, 2016

@author: acaproni
'''

from os import environ, getcwd, walk, path
from . import FileSupport
import logging 

class CommonDefs(object):
    """
    A collection of useful methods.
    Some of them could probably be moved somewhere else...
    """
    
    # Classpath separator for jars
    __classPathSeparator=":"
    
    @classmethod
    def buildClasspath(cls):
        """
        Build the class path by reading the jars from the
        IAS hierarchy of folders
        
        @return: A string with the jars in the classpath
        """
        
        # jars list is used to avoid duplications of jars in the classpath
        # It contains all the jars without the path
        # i.e. lc.jar but not ../lib/lc.jar
        jars=[]
        
        classpath=""
        FileSupport.FileSupport.getIASFolders()
        for folder in FileSupport.FileSupport.getIASSearchFolders('lib'):
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
