#! /usr/bin/env python3
'''
Created on Feb 9, 2018

@author: acaproni
'''

import unittest
from os import environ, access, R_OK
from os.path import exists, isfile, join, isdir
from shutil import rmtree

from IASLogging.logConf import Log
from IASTools.FileSupport import FileSupport
from IASTools.ModuleSupport import ModuleSupport


class TestCreateModule(unittest.TestCase):
    
    @classmethod
    def setUpClass(cls):
        cls.tmpFolder = environ['IAS_TMP_FOLDER']
        
        cls.moduleName = "ModuleForTest"
        
        cls.modulePath = join(cls.tmpFolder,cls.moduleName)
        
        logger.info("Test module: %s",cls.modulePath)
        
    @classmethod
    def tearDownClass(cls):
        if (exists(cls.modulePath)):
            logger.warning ("WARNING: module still exist %s",cls.modulePath)
            rmtree(cls.modulePath)
            if (exists(cls.modulePath)):
                logger.warning ("Cannot delete %s",cls.modulePath)
    
    def testTemplateExists(self):
        fileSupport = FileSupport("FoldersOfAModule.template","config")
        template = fileSupport.findFile()
        self.assertTrue(exists(template), "Template not found")
        self.assertTrue(isfile(template), "Template not file")
        self.assertTrue(access(template, R_OK), "Cannot read template file")
    
    def testModuleCreation(self):
        ModuleSupport.createModule(TestCreateModule.modulePath)
        self.assertTrue(exists(TestCreateModule.modulePath), "Module not created")
        self.assertTrue(isdir(TestCreateModule.modulePath), "Did not create a folder")
        
        ModuleSupport.removeExistingModule(TestCreateModule.modulePath)
        self.assertFalse(exists(TestCreateModule.modulePath), "Module not deleted")
    
    def testLicenseExists(self):
        '''
        Test if the license file exists in the created module
        '''
        ModuleSupport.createModule(TestCreateModule.modulePath)
        self.assertTrue(exists(TestCreateModule.modulePath), "Module not created")
        licenseFileName = join(TestCreateModule.modulePath,"LGPLv3.txt")
        self.assertTrue(exists(licenseFileName), "License file not found")
        ModuleSupport.removeExistingModule(TestCreateModule.modulePath)
        self.assertFalse(exists(TestCreateModule.modulePath), "Module not deleted")

if __name__ == '__main__':
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()
