#! /usr/bin/env python3
"""
Test the CdbFolders.py
"""
import os
import shutil

from IasCdb.CdbFolders import CdbFolders, Folders

class TestCdbFolders():
    """
    Test CdbFolder.

    It uses a temporary folder (self.temp_dir) that is removed at end.
    """
    def setup_method(self):
        self.temp_dir = "src/test/PyTestCDB"
        try:
            os.makedirs(os.path.join(self.temp_dir,"CDB"))
        except FileExistsError:
            # The folder should not exist: a leftover from a previous test?
            print(f"Warning: {os.path.join(self.temp_dir,'CDB')} should not exist")
        assert os.path.join(self.temp_dir,"CDB")

    def teardown_method(self):
        try:
            shutil.rmtree(self.temp_dir)
        except Exception as e: 
            print(f"Error deleting {os.path.join(self.temp_dir,'CDB')}:",e)
        pass 

    def test_static_get_folder_no_creation(self):
        folder = CdbFolders(Folders.SUPERVISOR, self.temp_dir).get_folder(False)
        assert "src/test/PyTestCDB/CDB/SUPERVISOR" == folder
        # Ensure the folder has not been created
        assert not os.path.isdir(folder)

    def test_static_get_and_create(self):
        folder = CdbFolders(Folders.SUPERVISOR, self.temp_dir).get_folder(True)
        assert "src/test/PyTestCDB/CDB/SUPERVISOR" == folder
        # Ensure the folder has not been created
        assert os.path.exists(folder)

    def test_cdb_folder_object(self):
        """
        Test the methods of an object by calling its method
        """
        cdb_folder =  CdbFolders(Folders.DASU, self.temp_dir)
        assert not os.path.isdir(cdb_folder.get_folder(False))
        assert not cdb_folder.exists()
        assert os.path.isdir(cdb_folder.get_folder(True))
        assert cdb_folder.exists()
        cdb_folder.delete()
        assert not cdb_folder.exists()
    
    def test_static_create_folders(self):
        """
        Test the creation of the main structure of the CDB
        i.e. all the subfolders
        """
        CdbFolders.create_folders(self.temp_dir)
        for folder in Folders:
            folder = CdbFolders(folder, self.temp_dir)
            assert folder.exists()
