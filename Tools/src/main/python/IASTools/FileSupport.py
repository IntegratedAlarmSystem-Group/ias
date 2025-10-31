'''
Created on Sep 23, 2016

@author: acaproni
'''
from os import environ, getcwd, walk, path, makedirs, access, W_OK, X_OK
from IASTools.DefaultPaths import DefaultPaths

class FileSupport(object):
    '''
    Support method for IAS files and folders
    '''
    # The map of types of files recognized by this tool and
    # the folder that contains the files of that type based on the
    # root of the module (typical case for development)
    iasModFileType = {
        "lib": "build/lib",
        "scalaTestClasses": "build/classes/scala/test/",
        "javaTestClasses":  "build/classes/java/test/",
        "bin": "build/bin",
        "config": "build/config",
        "src": "src"}
    # The map of types of files recognized by this tool and
    # the folder that contains the files of that type based on the
    # IAS_ROOT (typical case for operation)
    iasRootFileType = {
        "lib": "lib",
        "bin": "bin",
        "config": "config",
        "src": None }

    # IAS root from the environment
    __iasRoot = DefaultPaths.get_ias_root_folder()

    def __init__(self, fileName, fileType=None):
        '''
        Constructor
        
        @param filename: The mae of the file
        @param fileType: The type of the file (one in iasFileType) or None
        '''
        self.fileName = fileName
        self.fileType = fileType

    @classmethod
    def isValidType(cls, fileType):
        '''
        Check if the passed file type is supported.
        None is an accepted type

        :param fileType: the type to check (can be None)
        :return: True if the type has been recognized, False otherwise
        '''
        if fileType is None:
            return True
        fileType = fileType.lower()
        types = list(cls.iasModFileType.keys())
        return fileType in types
    
    def recursivelyLookForFile(self, folder):
        """
        Search recursively for a file in the given folder
        
        @param folder: The folder to search for the file
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
        * - build module folders (build/lib or build/bin for example)
        * - $IAS_ROOT folders
        
        @return: if the file is found The full path name of the file
                 otherwise throws a OSError exception
        """
        iasRoot=FileSupport.getIASRoot()
        if self.fileType:
            fileType = self.fileType.lower()
            if not fileType in FileSupport.iasModFileType:
                from IASLogging.logConf import Log
                _logger = Log.getLogger(__file__)
                _logger.error("Unrecognized fileType %s not in %s", fileType, str(FileSupport.iasModFileType))
                raise ValueError("Unrecognized fileType '" + fileType +"' not in " + str(FileSupport.iasModFileType))
            folders = (fileType,)
        else:
            # No folder passed: search in all the folders
            folders = FileSupport.iasModFileType
        
        for folder in folders:
            # Search in the current folder if it terminates with folder
            # i.e. search in lib if the name of the current folder terminates with lib
            currentFolder = getcwd()
            if currentFolder.endswith(folder):
                filePath = self.recursivelyLookForFile(currentFolder)
                if filePath is not None:
                    return filePath
                    
            # Assume to be in the root folder of a module
            # i.e. search in build/lib
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
    def getIASFolders(cls, fileType=None):
        '''
        Return the hierarchy of IAS folders.
        At the present it contains
        - the ./build/lib (typical case for development)
        - IAS root.
        
        The tuple returned is ordered by priority i.e. current module first
        
         @return A ordered tuple with the hierarchy of IAS folders
                 (at the present the current module and IAS_ROOT)
        '''
        if fileType is None:
            return (getcwd(), cls.getIASRoot())
        else:
            rootFolder = "%s/%s" % (cls.getIASRoot(), cls.iasRootFileType[fileType])
            modFolder = "%s/%s" % (getcwd(), cls.iasModFileType[fileType])
            return (modFolder, rootFolder)

    @classmethod
    def getClassFolders(cls):
        """
        Return the java and scala folders with test classes

        Class files are only in the local build/test folder
        :return:
        """
        return [cls.iasModFileType["scalaTestClasses"], cls.iasModFileType["javaTestClasses"]]

    @classmethod
    def createTmpFolder(cls):
        """
        Create the folder for temporary files as defined in IAS_TMP_FOLDER
        if it does not exists ALREADY
        
        An exception is thrown in case of error
        """
        tmpFolder=DefaultPaths.get_ias_tmp_folder()
        if not path.exists(tmpFolder):
            makedirs(tmpFolder)
        else:
            if not path.isdir(tmpFolder):
                from IASLogging.logConf import Log
                _logger = Log.getLogger(__file__)
                _logger.error("%s is not a directory", tmpFolder)
                raise OSError(tmpFolder+" is not a directory")
            # Can write?
            if not access(tmpFolder, W_OK | X_OK):
                from IASLogging.logConf import Log
                _logger = Log.getLogger(__file__)
                _logger.error("%s is not writable", tmpFolder)
                raise OSError(tmpFolder+" is not writable")
        
    @classmethod
    def createLogsFolder(cls):
        """
        Create the folder of los as defined in IAS_LOGS_FOLDER
        if it does not exists ALREADY
        
        An exception is thrown in case of error
        """
        logsFolder=DefaultPaths.get_ias_logs_folder()
        if not path.exists(logsFolder):
            makedirs(logsFolder)
        else:
            if not path.isdir(logsFolder):
                from IASLogging.logConf import Log
                _logger = Log.getLogger(__file__)
                _logger.error("%s is not a directory", logsFolder)
                raise OSError(logsFolder+" is not a directory")
            # Can write?
            if not access(logsFolder, W_OK | X_OK):
                from IASLogging.logConf import Log
                _logger = Log.getLogger(__file__)
                _logger.error("%s is not writable", logsFolder)
                raise OSError(logsFolder+" is not writable")
