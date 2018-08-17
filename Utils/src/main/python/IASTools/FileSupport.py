'''
Created on Sep 23, 2016

@author: acaproni
'''
from os import environ,getcwd, walk, path, makedirs, access, W_OK, X_OK
import logging


class FileSupport(object):
    '''
    Support method for IAS files and folders
    '''
    # Possible types of files recognized by this tool
    #
    # iasFileType matches with the folder names of a module
    # to ensure a valid value is given here
    iasFileType = ("lib", "bin","config","src")
    
    #IAS root from the environment
    __iasRoot=environ['IAS_ROOT']   
    

    def __init__(self, fileName,fileType=None):
        '''
        Constructor
        
        @param filename: The mae of the file
        @param fileType: The type of the file (one in iasFileType) or None
        '''
        self.fileName=fileName
        self.fileType=fileType
    
    def recursivelyLookForFile(self,folder):
        """
        Search recursively for a file in the given folder
        
        @param folder: The folder to search for the file
        @param name: the name of the file
        @return: the full name of the file if it exists,
                 None otherwise
        """
        for root, subFolders, files in walk(folder):
            for filePath in files:
                if (filePath == self.fileName):
                    return path.join(folder,filePath)
        return None
    
    def findFile(self):
        """
        Looks for a file named fileName in the hierarchy of IAS folders:
        * current folder (only if it ends with src for example)
        * module folders (lib for example)
        * parent module folders (../lib for example)
        * $IAS_ROOT folders
        
        The fileType tells where the file must be looked for and improves
        the response time of the script.
        
        
        @param fileName: The name of the file to look for
        @param fileType: the type (iasFileType) of file to look for (example BINARY,LIB...)
        @return: if the file is found The full path name of the file
                 otherwise throws a OSError exception
        """
        iasRoot=FileSupport.getIASRoot()
        if self.fileType:
            fileType = self.fileType.lower()
            if not fileType in FileSupport.iasFileType:
                logging.error("Unrecognized fileType %s not in %s",fileType,str(FileSupport.iasFileType))
                raise ValueError("Unrecognized fileType '"+fileType+"' not in "+str(FileSupport.iasFileType))
            folders = (fileType,)
        else:
            # No folder passed: search in all the folders
            folders = FileSupport.iasFileType
        
        for folder in folders:
            # Search in the current folder if its terminates with folder
            # i.e. search in lib if the name of the current folder terminates with lib
            currentFolder = getcwd()
            if currentFolder.endswith(folder):
                filePath = self.recursivelyLookForFile(currentFolder)
                if filePath is not None:
                    return filePath
                    
            # Assume to be in the root of a folder 
            ## i.e. search in ./lib
            folderToSearch=path.join(currentFolder,folder)
            if path.exists(folderToSearch) and path.isdir(folderToSearch):
                filePath = self.recursivelyLookForFile(folderToSearch)
                if filePath is not None:
                    return filePath
            # Assume to be in a  module folder and look for the file in the passed folder
            # for example current folder is ../src, search in ../lib
            folderToSearch=path.join(currentFolder,"..",folder)
            if path.exists(folderToSearch) and path.isdir(folderToSearch):
                filePath = self.recursivelyLookForFile(folderToSearch)
                if filePath is not None:
                    return filePath
            # Finally search in IAS_ROOT
            folderToSearch=path.join(iasRoot,folder)
            if path.exists(folderToSearch) and path.isdir(folderToSearch):
                filePath = self.recursivelyLookForFile(folderToSearch)
                if filePath is not None:
                    return filePath
        # Bad luck!
        raise FileNotFoundError(self.fileName+" not found")
    
    @classmethod
    def getIASRoot(cls):
        '''
        @return the IAS_ROOT
        '''
        return cls.__iasRoot
    
    @classmethod
    def getIASFolders(cls):
        '''
        Return the hierarchy of IAS folders.
        At the present it only contains the current module
        (the parent of the current folder i.e. assumes that
        the tscript runs in src that is the typical case
        for development) and IAS root; 
        in future can be expanded with one or more integration
        folders.
        
        The tuple returned is ordered by priority.
        
         @return A ordered tuple with the hierarchy of IAS folders
                 (at the present the current module and IAS_ROOT)
        '''
        return (path.join(getcwd(),".."),cls.getIASRoot())
    
    @classmethod
    def getIASSearchFolders(cls, fileType):
        """
        Return the folders to search for a a given file type
        
        @param fileType: the type (iasFileType) of file to look for (example BINARY,LIB...)
        @return a list with the order folders to search for a file of the given name
        """
        if fileType:
            fileType = fileType.lower()
            if not fileType in cls.iasFileType:
                logging.error("Invalid fileType %s not in %s", fileType,str(cls.iasFileType))
                raise ValueError("Invalid fileType '"+fileType+"' not in "+str(cls.iasFileType))
        else:
            # No folder passed: search in all the folders
            logging.error("Invalid fileType %s", fileType)
            raise ValueError("Invalid fileType "+fileType)
        
        folders = []
        for folder in cls.getIASFolders():
            folders.append(path.join(folder,fileType.lower()))
        return folders
    
    @classmethod
    def createTmpFolder(cls):
        """
        Create the folder for temporary files as defined in IAS_TMP_FOLDER
        if it does not exists ALREADY
        
        An exception is thrown in case of error
        """
        tmpFolder=environ["IAS_TMP_FOLDER"]
        if not path.exists(tmpFolder):
            makedirs(tmpFolder)
        else:
            if not path.isdir(tmpFolder):
                logging.error("%s is not a directory", tmpFolder)
                raise OSError(tmpFolder+" is not a directory")
            # Can write?
            if not access(tmpFolder, W_OK | X_OK):
                logging.error("%s is not writable", tmpFolder)
                raise OSError(tmpFolder+" is not writable")
        
    @classmethod
    def createLogsFolder(cls):
        """
        Create the folder of los as defined in IAS_LOGS_FOLDER
        if it does not exists ALREADY
        
        An exception is thrown in case of error
        """
        logsFolder=environ["IAS_LOGS_FOLDER"]
        if not path.exists(logsFolder):
            makedirs(logsFolder)
        else:
            if not path.isdir(logsFolder):
                logging.error("%s is not a directory", logsFolder)
                raise OSError(logsFolder+" is not a directory")
            # Can write?
            if not access(logsFolder, W_OK | X_OK):
                logging.error("%s is not writable", logsFolder)
                raise OSError(logsFolder+" is not writable")
        
