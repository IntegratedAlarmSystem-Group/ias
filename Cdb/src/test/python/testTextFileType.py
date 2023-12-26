#! /usr/bin/env python3
"""
Test the TextFileType.py
"""

import unittest

from IasCdb.TextFileType import TextFileType, FileType
from IasCdb.CdbFolders import CdbFolders

class TestTextFileType(unittest.TestCase):
    
    def setUp(self):
        self.json_cdb = "src/test/testJsonCdb/"
        self.yaml_cdb = "src/test/testYamlCdb"

    def tearDown(self):
        pass

    def testGetCdbType(self):
        self.assertEqual(TextFileType.get_cdb_type(self.json_cdb),FileType.JSON)
        self.assertEqual(TextFileType.get_cdb_type(self.yaml_cdb),FileType.YAML)

    def testGetTypeFromFile(self):
        self.assertEqual(TextFileType.from_file("test.json"),FileType.JSON)
        self.assertEqual(TextFileType.from_file("test.yaml"),FileType.YAML)
        self.assertIsNone(TextFileType.from_file("test.txt"))
        
if __name__ == '__main__':
    unittest.main()
