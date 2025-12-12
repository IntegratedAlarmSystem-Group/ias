#! /usr/bin/env python3
"""
Test the CdbReader.py
"""

import unittest
from IasCdb.Dao.IasDao import IasDao
from IasCdb.Dao.LogLevelDao import LogLevelDao
from IasCdb.CdbReader import CdbReader
from IasCdb.TextFileType import FileType, TextFileType

class TestCdbReader(unittest.TestCase):

    def setUp(self):
        self.json_cdb = "src/test/testJsonCdb/"
        self.yaml_cdb = "src/test/testYamlCdb"

    def tearDown(self):
        pass

    def testFileType(self):
        self.assertEqual(CdbReader(self.json_cdb).cdbTxtFiles.files_type, FileType.JSON)
        self.assertEqual(CdbReader(self.yaml_cdb).cdbTxtFiles.files_type, FileType.YAML)

    def testGetIasJson(self):
        reader = CdbReader(self.json_cdb)
        ias = reader.get_ias()
        self.assertIsNotNone(ias)
        self.assertEqual(ias.log_level, LogLevelDao.INFO)
        self.assertEqual(ias.refresh_rate,5)
        self.assertEqual(ias.validity_threshold,11)
        self.assertEqual(ias.hb_frequency, 10)
        self.assertEqual(ias.smtp, 'acaproni:pswd@smtp.test.org')
        self.assertEqual(ias.bsdb_url, '127.0.0.1:9092')

        props = { 'PropName1':'PropValue1', 'PropName2':'PropValue2'}
        self.assertEqual(ias.props, props)

    def testGetIasYaml(self):
        reader = CdbReader(self.yaml_cdb)
        ias = reader.get_ias()
        self.assertIsNotNone(ias)
        self.assertEqual(ias.log_level, LogLevelDao.INFO)
        self.assertEqual(ias.refresh_rate,5)
        self.assertEqual(ias.validity_threshold,11)
        self.assertEqual(ias.hb_frequency, 10)
        self.assertEqual(ias.smtp, 'acaproni:pswd@smtp.test.org')
        self.assertEqual(ias.bsdb_url, '127.0.0.1:9092')

        props = { 'P1-Name':'A value for property 1', 'P2-Name':'A value for another property'}
        self.assertEqual(ias.props, props)

if __name__ == '__main__':
    unittest.main()
