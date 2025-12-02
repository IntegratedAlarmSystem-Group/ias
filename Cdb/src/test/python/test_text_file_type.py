"""
Test the TextFileType.py
"""

from IasCdb.TextFileType import TextFileType, FileType
from IasCdb.CdbFolders import CdbFolders

class TestTextFileType():
    
    def setup_method(self):
        self.json_cdb = "src/test/testJsonCdb/"
        self.yaml_cdb = "src/test/testYamlCdb"

    def teardown_method(self):
        pass

    def testGetCdbType(self):
        assert TextFileType.get_cdb_type(self.json_cdb) == FileType.JSON
        assert TextFileType.get_cdb_type(self.yaml_cdb) == FileType.YAML

    def testGetTypeFromFile(self):
        assert TextFileType.from_file("test.json") == FileType.JSON
        assert TextFileType.from_file("test.yaml") == FileType.YAML
        assert TextFileType.from_file("test.txt") is None
