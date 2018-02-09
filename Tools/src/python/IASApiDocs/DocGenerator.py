'''

Base class for java, scala and python API docs generators
Created on Jul 7, 2017

@author: acaproni
'''

import sys
import os

class DocGenerator(object):
    '''
    The base class for API docs generators
    '''


    def __init__(self,srcFolder,dstFolder,outFile=sys.stdout):
        """ 
        Constructor
        @param srcFolder: the folder with sources to generate their documentation
        @param dstFolder: destination folder for the api docs  
        @param outFile: the file where the output generated by calling java/scala/py-doc must be sent
        """
        self.checkFolders(srcFolder,dstFolder)
        self.srcFolder=srcFolder
        self.dstFolder=dstFolder
        self.outFile=outFile
        assert self.outFile is not None
        
    def checkFolders(self,src,dst):
        """
        Check if the source and dest folders are valid and if it is not the case,
        throws an exception
        @param src: the folder with java sources to check
        @param dst: destination folder to check
        """
        # Check if src folder exists
        if not os.path.exists(src):
            raise OSError("The source folder", src,"does not exist")
        elif not os.path.isdir(src):
            raise OSError("The source folder", src,"is not a directory")
            
        # Check if the destination folder exists
        if not os.path.exists(dst):
            os.mkdir(dst)
        if not os.path.exists(dst):
            raise OSError("The destination folder", dst,"does not exist")
        elif not os.path.isdir(dst):
            raise OSError("The destination folder", dst,"is not a directory")
        
    def containsSources(self,folder,fileExtension):
        '''
        @param folder: the folder (src or test) to check if contains java sources
        @param fileExtension: the extension of the files that the folder is supposed to contain
        @return: True if the passed folder contains java sources
        ''' 
        for root, subdirs, files in os.walk(folder):
            for file in files:
                if file.endswith(fileExtension):
                    return True
        return False
        
    def getSrcPaths(self,sourceFolder, includeTestFolder,folderName,fileExtension):
        """
        Scan the source folder and return a list of source folders
        containg java files.
        Java source can be contained into src or test (the latter is used only 
        if the includeTestFolder parameter is True)
        The search is recursive because a folder can contains several modules
        
        @param sourceFolder: root source folder (generally IAS, passed in the command line)
        @param includeTestFolder: True to inculde test folders in the scan
        @param  folderName: the name of the folder containing the sources like java or python 
        @param fileExtension: the extension of the files that the folder is supposed to contain
        """
        ret = []
        for root, subdirs, files in os.walk(sourceFolder):
            if root.endswith(os.path.sep+"src/"+folderName) or (includeTestFolder and root.endswith(os.path.sep+"test/"+folderName)):
                if self.containsSources(root,fileExtension):
                    ret.append(root)
        return ret