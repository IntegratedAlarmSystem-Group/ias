#! /usr/bin/env python3
"""
Test the CdbReader.py
"""

import unittest
from IasCdb.Dao.IasDao import IasDao
from IasCdb.Dao.LogLevelDao import LogLevelDao
from IasCdb.Dao.SoundTypeDao import SoundTypeDao
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

    def testGetIasiosJson(self):
        reader = CdbReader(self.json_cdb)
        iasios = reader.get_iasios()
        self.assertIsNotNone(iasios)
        self.assertEqual(len(iasios),16)

    def testGetIasiosYaml(self):
        reader = CdbReader(self.yaml_cdb)
        iasios = reader.get_iasios()
        self.assertIsNotNone(iasios)
        self.assertEqual(len(iasios),16)

    def testGetIasioJson(self):
        reader = CdbReader(self.json_cdb)
        self.assertIsNone(reader.get_iasio("UNKNOWN-ID"))
        iasio = reader.get_iasio("OUTPUT-ID")
        self.assertIsNotNone(iasio)
        self.assertEqual(iasio.docUrl, "http://wiki.alma.cl/outputID")
        self.assertEqual(iasio.shortDesc, "ID of output")
        self.assertEqual(iasio.iasType.value, "ALARM")
        self.assertEqual(iasio.id, "OUTPUT-ID")
        self.assertIsNone(iasio.emails)
        self.assertEqual(iasio.sound.value, SoundTypeDao.NONE)
        self.assertEqual(iasio.canShelve, False)
        self.assertEqual(iasio.templateId, None)

        iasio = reader.get_iasio("SoundInput")
        self.assertIsNotNone(iasio)
        self.assertEqual(iasio.id, "SoundInput")
        self.assertEqual(iasio.templateId, "templated-input")
        self.assertEqual(iasio.shortDesc, "Templated input with sound example")
        self.assertEqual(iasio.iasType.value, "ALARM")
        self.assertEqual(iasio.sound.value, SoundTypeDao.TYPE2)
        self.assertEqual(iasio.canShelve, True)
        self.assertEqual(iasio.emails, "stencccr@yujiehanjiao.cc")
        self.assertIsNone(iasio.docUrl)

    def testGetIasioYaml(self):
        reader = CdbReader(self.yaml_cdb)
        self.assertIsNone(reader.get_iasio("UNKNOWN-ID"))
        iasio = reader.get_iasio("OUTPUT-ID")
        self.assertIsNotNone(iasio)
        self.assertEqual(iasio.docUrl, "http://wiki.alma.cl/outputID")
        self.assertEqual(iasio.shortDesc, "ID of output")
        self.assertEqual(iasio.iasType.value, "ALARM")
        self.assertEqual(iasio.id, "OUTPUT-ID")
        self.assertIsNone(iasio.emails)
        self.assertEqual(iasio.sound.value, SoundTypeDao.NONE)
        self.assertEqual(iasio.canShelve, False)
        self.assertEqual(iasio.templateId, None)

        iasio = reader.get_iasio("SoundInput")
        self.assertIsNotNone(iasio)
        self.assertEqual(iasio.id, "SoundInput")
        self.assertEqual(iasio.templateId, "templated-input")
        self.assertEqual(iasio.shortDesc, "Templated input with sound example")
        self.assertEqual(iasio.iasType.value, "ALARM")
        self.assertEqual(iasio.sound.value, SoundTypeDao.TYPE2)
        self.assertEqual(iasio.canShelve, True)
        self.assertEqual(iasio.emails, "stencccr@yujiehanjiao.cc")
        self.assertIsNone(iasio.docUrl)
        
        
if __name__ == '__main__':
    unittest.main()
