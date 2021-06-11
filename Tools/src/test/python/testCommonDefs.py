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

    extJarsDirs = environ['IAS_EXTERNAL_JARS']

    extJarFileName = extJarsDirs+"/test.jar"

    @classmethod
    def setUpClass(cls):

        logger.info("Using ext jars folder %s",TestCommonDefs.extJarsDirs)
        if not path.exists(TestCommonDefs.extJarsDirs):
            logger.info('Creating folder %s',TestCommonDefs.extJarsDirs)
            mkdir(TestCommonDefs.extJarsDirs)
        logger.info("Adding a JAR to IAS_EXTERNAL_JARS")
        f= open(TestCommonDefs.extJarFileName,"w+")
        for i in range(10):
            f.write("This is line %d\r\n" % (i+1))
        f.close()
        assert path.exists(TestCommonDefs.extJarFileName)

    @classmethod
    def tearDownClass(cls):
        if path.exists(TestCommonDefs.extJarsDirs):
            logger.info('Deleting folder %s',TestCommonDefs.extJarsDirs)
            rmtree(TestCommonDefs.extJarsDirs)

    def testImportOfExtJars(self):
        ''' Checks if the jars in the IAS_EXTERNAL_JARS are added to the classpath '''
        logger.info("Importing of external jar from %s",TestCommonDefs.extJarsDirs)
        cp = CommonDefs.buildClasspath()
        logger.info("Classpath = %s",cp)
        self.assertTrue(TestCommonDefs.extJarFileName in cp)

if __name__ == '__main__':
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()
