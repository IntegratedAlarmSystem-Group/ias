#! /usr/bin/env python3
"""
Test the CdbTxtFiles.py
"""
import unittest
import pathlib

from IasCdb.CdbTxtFiles import CdbTxtFiles
from IasCdb.TextFileType import FileType

class TestCdbFiles(unittest.TestCase):

    def setUp(self):
        self.json_cdb = "src/test/testJsonCdb/"
        self.yaml_cdb = "src/test/testYamlCdb"

    def tearDown(self):
        pass

    def testGetJsonIas(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_ias_file_path()
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlIas(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_ias_file_path()
        self.assertTrue(pathlib.Path(file_path))

    def testGetJsonSuperv(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_supervisor_file_path("Supervisor-ID1")
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlSuperv(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_supervisor_file_path("Supervisor-ID1")
        self.assertTrue(pathlib.Path(file_path))


    def testGetJsonDasu(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_supervisor_file_path("DasuID2")
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlDasu(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_supervisor_file_path("DasuID2")
        self.assertTrue(pathlib.Path(file_path))

    def testGetJsonAsce(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_supervisor_file_path("ASCE-WITH-TEMPLATED-INPUTS")
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlAsce(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_supervisor_file_path("ASCE-WITH-TEMPLATED-INPUTS")
        self.assertTrue(pathlib.Path(file_path))

    def testGetJsonIasio(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_iasio_file_path()
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlIasio(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_iasio_file_path()
        self.assertTrue(pathlib.Path(file_path))

    def testGetJsonTf(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_tf_file_path()
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlTf(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_tf_file_path()
        self.assertTrue(pathlib.Path(file_path))
    
    def testGetJsonClient(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_client_file_path("test")
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlClient(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_client_file_path("test")
        self.assertTrue(pathlib.Path(file_path))

    def testGetJsonPlugin(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_plugin_file_path("PluginID2")
        self.assertTrue(pathlib.Path(file_path))

    def testGetYamlPlugin(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_plugin_file_path("PluginID2")
        self.assertTrue(pathlib.Path(file_path))

    def testGuessingCdbType(self):
        j_reader = CdbTxtFiles.from_folder(self.json_cdb)
        self.assertEqual(j_reader.files_type, FileType.JSON)
        y_reader = CdbTxtFiles.from_folder(self.yaml_cdb)
        self.assertEqual(y_reader.files_type, FileType.YAML)

if __name__ == '__main__':
    unittest.main()
