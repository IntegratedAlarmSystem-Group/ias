#! /usr/bin/env python3
'''
Created on Mar 17, 2020

@author: acaproni
'''

import unittest
from os import environ, path, mkdir
from shutil import rmtree

from IASLogging.logConf import Log
from IASTools.CommonDefs import CommonDefs

class TestCommonDefs(unittest.TestCase):

    iasExtJarsEnvVar = environ['IAS_EXTERNAL_JARS']

    extJarsDirs = iasExtJarsEnvVar.split(":")

    baseFileName = "test"

    jarExtension = ".jar"

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
    def setUpClass(cls):
        logger.info("Using ext jars folders %s",str(TestCommonDefs.extJarsDirs))
        t = 1
        for folder in TestCommonDefs.extJarsDirs:
            if not path.exists(folder):
                logger.info('Creating folder %s',folder)
                mkdir(folder)
                logger.info("Adding a JAR to %s",folder)
                addedJar = TestCommonDefs.addJar(
                    folder,
                    TestCommonDefs.baseFileName+str(t),
                    TestCommonDefs.jarExtension)
                TestCommonDefs.jars.append(addedJar)
                logger.info("File %s added", addedJar)
                t = t + 1
            else:
                logger.error("Folder %s already exists", folder)
        logger.info("Jar in IAS_EXTERNAL_JARS to check %s",str(TestCommonDefs.jars))
        

    @classmethod
    def tearDownClass(cls):
        for folder in  TestCommonDefs.extJarsDirs:
            if path.exists(folder):
                logger.info('Deleting folder %s',folder)
                rmtree(folder)
            else:
                logger.error("Folder %s NOT found", folder)

    def testImportOfExtJars(self):
        ''' Checks if the jars in the IAS_EXTERNAL_JARS folders are added to the classpath '''
        logger.info("Importing of external jar from %s",str(TestCommonDefs.extJarsDirs))
        self.assertEqual(len(TestCommonDefs.extJarsDirs), 2)
        cp = CommonDefs.buildClasspath()
        logger.info("Classpath = %s",cp)
        for jar in TestCommonDefs.jars:
            self.assertTrue(jar in cp)

if __name__ == '__main__':
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()
