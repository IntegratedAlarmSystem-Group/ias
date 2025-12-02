#! /usr/bin/env python3
'''
Created on Mar 17, 2020

@author: acaproni
'''

from logging import Logger
from os import environ, path, mkdir
from shutil import rmtree

from IASLogging.logConf import Log
from IASTools.CommonDefs import CommonDefs

class TestCommonDefs():

    

    # The list of the jars created into the external jars folders
    jars = []

    @classmethod
    def addJar(cls, folder, fileName, extension):
        name = folder+"/"+fileName+extension 
        f= open(name,"w+")
        for i in range(10):
            f.write("This is line %d\r\n" % (i+1))
        f.close()
        assert path.exists(name)
        return name

    @classmethod
    def setup_class(cls):
        cls.Logger = Log.getLogger(__file__)

        # Steth the environment variable IAS_EXTERNAL_JARS
        pwd = environ['PWD']
        environ['IAS_EXTERNAL_JARS']=f"{pwd}/externalJars1:{pwd}/externalJars2"

        cls.iasExtJarsEnvVar = environ['IAS_EXTERNAL_JARS']

        cls.extJarsDirs = cls.iasExtJarsEnvVar.split(":")

        cls.baseFileName = "test"

        cls.jarExtension = ".jar"

        cls.Logger.info("Using ext jars folders %s",str(cls.extJarsDirs))

        t = 1
        for folder in cls.extJarsDirs:
            if not path.exists(folder):
                cls.Logger.info('Creating folder %s',folder)
                mkdir(folder)
                cls.Logger.info("Adding a JAR to %s",folder)
                addedJar = cls.addJar(
                    folder,
                    cls.baseFileName+str(t),
                    cls.jarExtension)
                cls.jars.append(addedJar)
                cls.Logger.info("File %s added", addedJar)
                t = t + 1
            else:
                cls.Logger.error("Folder %s already exists", folder)
        cls.Logger.info("Jar in IAS_EXTERNAL_JARS to check %s",str(cls.jars))
        

    @classmethod
    def teardown_class(cls):
        for folder in  cls.extJarsDirs:
            if path.exists(folder):
                cls.Logger.info('Deleting folder %s',folder)
                rmtree(folder)
            else:
                cls.Logger.error("Folder %s NOT found", folder)

    def test_import_ext_jars(self):
        ''' Checks if the jars in the IAS_EXTERNAL_JARS folders are added to the classpath '''
        TestCommonDefs.Logger.info("Importing of external jar from %s",str(TestCommonDefs.extJarsDirs))
        assert len(TestCommonDefs.extJarsDirs) == 2
        cp = CommonDefs.buildClasspath()
        TestCommonDefs.Logger.info("Classpath = %s",cp)
        for jar in TestCommonDefs.jars:
            assert jar in cp
