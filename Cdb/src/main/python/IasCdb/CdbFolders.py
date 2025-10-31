"""
Python implementation of CdbFolders.java
"""

import os
import shutil

from enum import Enum

class Folders(Enum):
    ROOT = 1
    SUPERVISOR = 2
    DASU = 3
    ASCE = 4
    IASIO = 5
    TF = 6
    TEMPLATE = 7
    PLUGIN = 8
    CLIENT =9

class CdbFolders:
    '''
    CdbFolder is the python translation of the java CdbFolder.java

    The differences are mostly due to the fact that python implemntation 
    of enumerated differs from java 
    '''

    ALL_FOLDERS = {
        Folders.ROOT:"CDB",
        Folders.SUPERVISOR:"SUPERVISOR",
        Folders.DASU:"DASU",
        Folders.ASCE:"ASCE",
        Folders.IASIO:"IASIO",
        Folders.TF:"TF",
        Folders.TEMPLATE:"TEMPLATE",
        Folders.PLUGIN:"PLUGIN",
        Folders.CLIENT:"CLIENT"
    }

    def __init__(self, folder: Folders, cdb_parent_path: str) -> None:
        '''
        Constructor

        Args:
            folder: the forlder of this object
            cdb_parent_path: the path of the folder containig the CDB
        '''
        self.folder = folder
        self.cdb_parent_path = cdb_parent_path
        if not self.cdb_parent_path:
            raise ValueError("The parent path can't be null/empty")
        if self.folder==Folders.ROOT:
            self.folder_path = os.path.join(cdb_parent_path, CdbFolders.ALL_FOLDERS[Folders.ROOT])
        else:
            self.folder_path = os.path.join(cdb_parent_path, CdbFolders.ALL_FOLDERS[Folders.ROOT], CdbFolders.ALL_FOLDERS[folder])
        if not os.path.isdir(self.cdb_parent_path):
            raise ValueError(f"CDB parent folder {self.cdb_parent_path} does not exist!")

    def delete(self) -> bool:
        """
        Delete the subfolder represented by this object, if it exists.
	    
        Returns:
	        True if the folder has been deleted, False otherwise
        """
        if not os.path.isdir(self.folder_path):
            # The folder does not exist ==> nothing to do
            return False
        
        self._delete_folder()
        return True

    def _delete_folder(self) -> None:
        """
        Recursively delete a folder from the file system.
        Args:
            folder_to_delete: The folder to delete
        """
        if not os.path.isdir(self.folder_path):
            # The folder does not exist ==> nothing to do
            return        
        for root, dirs, files in os.walk(self.folder_path, topdown=False):
            for file in files:
                os.remove(os.path.join(root, file))
            for dir in dirs:
                os.rmdir(os.path.join(root, dir))
        os.rmdir(self.folder_path)

    def get_folder(self, create: bool) -> str:
        """
        Get the path of the CDB sub folder and if it is the case, creates it.

        Args:
            create: if True and the subfolder does not exist, then create it
        Returns:
            the path to the subfolder.
        """
        # does not exist: create if requested
        if create and not os.path.isdir(self.folder_path):
            os.makedirs(self.folder_path)
        return self.folder_path

    def exists(self) -> bool:
        """
        Check if the subfolder exists

        Returns:
            True if the folder exists, False otherwise
        """
        return os.path.exists(self.folder_path)

    @staticmethod
    def get_subfolder(cdb_parent_path: str, folder: Folders, create: bool) -> str:
        """
        Get the path of a CDB folder and if it is the case, creates the folder

        Args:
            cdbParentPath: The path to the parent of the CDB
            folder: the CDB folder to create
            create: if True and the subfolder does not exist, then create it
        Returns:
            the path to the subfolder
        """
        if not folder:
            raise ValueError("CDB subfolder can't be None")
        if not cdb_parent_path:
            raise ValueError("CDB parent folder can't be None")
        cdb_folders = CdbFolders(folder, cdb_parent_path)
        return cdb_folders.get_folder(create)

    @staticmethod
    def create_folders(cdb_parent_path: str):
        """
        Create all the subfolders of the CDB.
        If a subfolder already exists then nothing is done

        Args:
            cdbParentPath: The path to the parent of the CDB
        """
        if not cdb_parent_path:
            raise ValueError("CDB parent folder can't be None")
        for folder in CdbFolders.ALL_FOLDERS:
            folder = CdbFolders(folder, cdb_parent_path)
            folder.get_folder(True)
