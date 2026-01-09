#! /usr/bin/env python3
'''
Created on Feb 9, 2018

@author: acaproni
'''

from os import environ, access, R_OK
from os.path import exists, isfile, join, isdir
from shutil import rmtree

from IASLogging.logConf import Log
from IASTools.FileSupport import FileSupport
from IASTools.ModuleSupport import ModuleSupport


class TestCreateModule():

    @classmethod
    def setup_class(cls):
        cls.Logger = Log.getLogger(__file__)
        cls.Logger.info("Starting TestCreateModule tests")
    
    def setup_method(self):
        self.tmpFolder = environ.get('IAS_TMP_FOLDER','/tmp')
        
        self.moduleName = "ModuleForTest"
        
        self.modulePath = join(self.tmpFolder,self.moduleName)

        TestCreateModule.Logger = Log.getLogger(__file__)
        
    def teardown_method(self):
        if (exists(self.modulePath)):
            TestCreateModule.Logger.warning ("WARNING: module still exist %s",self.modulePath)
            rmtree(self.modulePath)
            if (exists(self.modulePath)):
                TestCreateModule.Logger.warning ("Cannot delete %s",self.modulePath)
    
    def test_template_exists(self):
        TestCreateModule.Logger.info("Testing if template exists")
        fileSupport = FileSupport("FoldersOfAModule.template","config")
        template = fileSupport.findFile()
        assert exists(template), "Template not found"
        assert isfile(template), "Template not file"
        assert access(template, R_OK), "Cannot read template file"
    
    def test_module_creation(self):
        TestCreateModule.Logger.info("Testing module creation %s",self.modulePath)
        ModuleSupport.createModule(self.modulePath)
        assert exists(self.modulePath), "Module not created"
        assert isdir(self.modulePath), "Did not create a folder"
        
        ModuleSupport.removeExistingModule(self.modulePath)
        assert not exists(self.modulePath), "Module not deleted"
    
    def test_license_exists(self):
        '''
        Test if the license file exists in the created module
        '''
        TestCreateModule.Logger.info("Testing if license exists")
        ModuleSupport.createModule(self.modulePath)
        assert exists(self.modulePath), "Module not created"
        licenseFileName = join(self.modulePath,"LGPLv3.txt")
        assert exists(licenseFileName), "License file not found"
        ModuleSupport.removeExistingModule(self.modulePath)
        assert not exists(self.modulePath), "Module not deleted"
