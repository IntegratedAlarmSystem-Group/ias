#! /usr/bin/env python
'''
Created on Feb 9, 2018
@author: acaproni
'''

import unittest

from os import environ, access, R_OK
from os.path import exists, isfile, join, isdir
from IASTools.FileSupport import FileSupport
from IASTools.ModuleSupport import ModuleSupport
from shutil import rmtree

#Import logging
import logging
import sys
import os, errno

class TestCreateModule(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.tmpFolder = environ['IAS_TMP_FOLDER']
        cls.moduleName = "ModuleForTest"
        cls.modulePath = join(cls.tmpFolder,cls.moduleName)
        logger.info("Set the temp folder for test")
        print("Test module:",cls.modulePath)

    @classmethod
    def tearDownClass(cls):
        if (exists(cls.modulePath)):
            logger.warning("WARNING: module still exist")
            print ("WARNING: module still exist",cls.modulePath)
            rmtree(cls.modulePath)
            if (exists(cls.modulePath)):
                print ("Cannot delete",cls.modulePath)

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
        """
        Insert the configuration for the logger
        """
        LEVELS = {'debug': logging.DEBUG,'info': logging.INFO,'warning': logging.WARNING,'error': logging.ERROR,'critical': logging.CRITICAL}

        if len(sys.argv) > 1:
            level_name = sys.argv[1]
            level = LEVELS.get(level_name, logging.NOTSET)
            logging.basicConfig(level=level)

        #Define logger with logging import
        logger = logging.getLogger()

        #Set the level of the message visualize
        logger.setLevel(logging.DEBUG)

        #Set the format of the log
        logFormatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")
        #Set path where save the file and the name of the file.
        logPath="../IAS_LOGS_FOLDER"
        fileName="TestCreateModule"
        try:
            os.makedirs(logPath)
        except OSError as e:
            if e.errno != errno.EEXIST:
                raise
        fileHandler = logging.FileHandler("{0}/{1}.log".format(logPath, fileName))
        fileHandler.setFormatter(logFormatter)
        logger.addHandler(fileHandler)

        #Start stream for write into file, from here when it's insert the logger. write all into file.
        consoleHandler = logging.StreamHandler()
        consoleHandler.setFormatter(logFormatter)
        logger.addHandler(consoleHandler)
        logger.info("start main")
        unittest.main()
