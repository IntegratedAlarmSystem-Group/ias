#! /usr/bin/env python3
"""
Test the CdbFolders.py
"""
import unittest
import os
import shutil

from IasCdb.CdbFolders import CdbFolders, Folders

class TestCdbFolders(unittest.TestCase):
    """
    Test CdbFolder.

    It uses a temporary folder (self.temp_dir) that is removed at end.
    """
    def setUp(self):
        self.temp_dir = "src/test/PyTestCDB"
        try:
            os.makedirs(os.path.join(self.temp_dir,"CDB"))
        except FileExistsError:
            # The folder should not exist: a leftover from a previous test?
            print(f"Warning: {os.path.join(self.temp_dir,'CDB')} should not exist")
        self.assertTrue(os.path.join(self.temp_dir,"CDB"))

    def tearDown(self):
        try:
            shutil.rmtree(self.temp_dir)
        except Exception as e: 
            print(f"Error deleting {os.path.join(self.temp_dir,'CDB')}:",e)
        pass 

    def testStaticGetFolderNoCreation(self):
        folder = CdbFolders(Folders.SUPERVISOR, self.temp_dir).get_folder(False)
        self.assertEqual("src/test/PyTestCDB/CDB/SUPERVISOR", folder)
        # Ensure the folder has not been created
        self.assertFalse(os.path.isdir(folder))

    def testStaticGetAndCreate(self):
        folder = CdbFolders(Folders.SUPERVISOR, self.temp_dir).get_folder(True)
        self.assertEqual("src/test/PyTestCDB/CDB/SUPERVISOR", folder)
        # Ensure the folder has not been created
        self.assertTrue(os.path.exists(folder))

    def testCdbFolderObject(self):
        """
        Test the methods of an object by calling its method
        """
        cdb_folder =  CdbFolders(Folders.DASU, self.temp_dir)
        self.assertFalse(os.path.isdir(cdb_folder.get_folder(False)))
        self.assertFalse(cdb_folder.exists())
        self.assertTrue(os.path.isdir(cdb_folder.get_folder(True)))
        self.assertTrue(cdb_folder.exists())
        cdb_folder.delete()
        self.assertFalse(cdb_folder.exists())
    
    def testStaticCreateFolders(self):
        """
        Test the creation of the main structure of the CDB
        i.e. all the subfolders
        """
        CdbFolders.create_folders(self.temp_dir)
        for folder in Folders:
            folder = CdbFolders(folder, self.temp_dir)
            self.assertTrue(folder.exists())

if __name__ == '__main__':
    unittest.main()
