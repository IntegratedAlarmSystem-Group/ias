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

    ALL_FOLDERS = {
        Folders.ROOT:"CDB",
        Folders.SUPERVISOR:"Supervisor",
        Folders.DASU:"DASU",
        Folders.ASCE:"ASCE",
        Folders.IASIO:"IASIO",
        Folders.TF:"TF",
        Folders.TEMPLATE:"TEMPLATE",
        Folders.PLUGIN:"PLUGIN",
        Folders.CLIENT:"CLIENT"
    }


    def __init__(self, folder_name):
        """
        Constructor
        Args:
        cdb_parent_path: the parent folder of the CDB
        """
        self.folder_name = folder_name

    def delete(self, cdb_parent_path) -> bool:
        """
        Delete the subfolder, if it exists.
	    Args:* 
	      folderToDelete: The subfolder to delete
        Returns
	        True if the folder has been deleted,
	        False otherwise
        """
        if cdb_parent_path is None:
            raise ValueError("The parent path of the CDB can't be None")
        
        folder_path = self._build_path(cdb_parent_path)
        
        if not self.exists(cdb_parent_path):
            # The folder does not exist ==> nothing to do
            return False
        
        self._delete_folder(folder_path)
        return True

    def _delete_folder(self, folder_to_delete):
        """
        Recursively delete a folder from the file system.
        Args:
            folder_to_delete: The folder to delete
        """
        if folder_to_delete is None:
            raise ValueError("Cannot delete a NULL folder")
        
        for root, dirs, files in os.walk(folder_to_delete, topdown=False):
            for file in files:
                os.remove(os.path.join(root, file))
            for dir in dirs:
                os.rmdir(os.path.join(root, dir))
        os.rmdir(folder_to_delete)

    def _build_path(self, cdb_parent_path) -> str:
        """
        Build the path of the subfolde

        Args:
            cdb_parent_path: The path to the parent of the CDB
        Returns:
            The path to the subfolder
        """
        if cdb_parent_path is None:
            raise ValueError("The parent path of the CDB can't be None")
        
        if self.folder_name == self.ROOT:
            folder_path = os.path.join(cdb_parent_path, self.ROOT)
        else:
            folder_path = os.path.join(cdb_parent_path, self.ROOT, self.folder_name)
        
        return folder_path

    def get_folder(self, cdb_parent_path, create):
        """
        Get the path of the CDB folder and if it is the case, creates it.

        Args:
            cdbParentPath: The path to the parent of the CDB
            create: if True and the subfolder does not exist, then create it
        Returns:
            the path to the subfolder.
        """
        if cdb_parent_path is None:
            raise ValueError("The parent path of the CDB can't be None")
        
        folder_path = self._build_path(cdb_parent_path)
        if self.exists(cdb_parent_path):
            if os.path.isdir(folder_path) and os.access(folder_path, os.W_OK):
                # No need to create the folder
                return folder_path
            else:
                raise IOError(f"{folder_path} exists but is unusable: check permissions and type")
        else:
            os.makedirs(folder_path)
            return folder_path

    def exists(self, cdb_parent_path: str) -> bool:
        """
        Check if the subfolder exists

        Args:
            cdbParentPath The path of the folder
        Returns:
            True if the folder exists, False otherwise
        """
        path = self._build_path(cdb_parent_path)
        return os.path.exists(path)

    @staticmethod
    def get_subfolder(cdb_parent_path, folder, create):
        """
        Get the path of a CDB folder and if it is the case, creates the folder

        Args:
            cdbParentPath: The path to the parent of the CDB
            folder: the CDB folder to create
            create: if True and the subfolder does not exist, then create it
        Returns:
            the path to the subfolder
        """
        if folder is None:
            raise ValueError("CDB subfolder can't be None")
        if cdb_parent_path is None:
            raise ValueError("CDB parent folder can't be None")
        cdb_folders =CdbFolders(cdb_parent_path)
        return cdb_folders.get_folder(folder, create)

    @staticmethod
    def create_folders(cdb_parent_path: str):
        """
        Create all the subfolders of the CDB.
        If a subfolder already exists then nothing is done

        Args:
            cdbParentPath: The path to the parent of the CDB
        """
        cdb_folders =CdbFolders(cdb_parent_path)
        for folder in CdbFolders.ALL_FOLDERS:
            cdb_folders.get_folder(cdb_parent_path, True)
