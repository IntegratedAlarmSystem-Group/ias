"""
Test the CdbTxtFiles.py
"""
import pathlib

from IasCdb.CdbTxtFiles import CdbTxtFiles
from IasCdb.TextFileType import FileType

class TestCdbFiles():

    def setup_method(self):
        self.json_cdb = "src/test/testJsonCdb/"
        self.yaml_cdb = "src/test/testYamlCdb"

    def teardown_method(self):
        pass

    def test_get_Json_ias(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_ias_file_path()
        assert pathlib.Path(file_path)

    def test_get_Yaml_ias(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_ias_file_path()
        assert pathlib.Path(file_path)

    def test_get_json_superv(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_supervisor_file_path("Supervisor-ID1")
        assert pathlib.Path(file_path)

    def test_get_yaml_superv(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_supervisor_file_path("Supervisor-ID1")
        assert pathlib.Path(file_path)


    def test_get_json_dasu(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_supervisor_file_path("DasuID2")
        assert pathlib.Path(file_path)

    def test_get_yaml_dasu(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_supervisor_file_path("DasuID2")
        assert pathlib.Path(file_path)

    def test_get_json_asce(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_supervisor_file_path("ASCE-WITH-TEMPLATED-INPUTS")
        assert pathlib.Path(file_path)

    def test_get_yaml_asce(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_supervisor_file_path("ASCE-WITH-TEMPLATED-INPUTS")
        assert pathlib.Path(file_path)

    def test_get_json_iasio(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_iasio_file_path()
        assert pathlib.Path(file_path)

    def test_get_yaml_iasio(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_iasio_file_path()
        assert pathlib.Path(file_path)

    def test_get_json_tf(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_tf_file_path()
        assert pathlib.Path(file_path)

    def test_get_yaml_tf(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_tf_file_path()
        assert pathlib.Path(file_path)
    
    def test_get_json_client(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_client_file_path("test")
        assert pathlib.Path(file_path)

    def test_get_yaml_client(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_client_file_path("test")
        assert pathlib.Path(file_path)

    def test_get_json_plugin(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.JSON)
        file_path=reader.get_plugin_file_path("PluginID2")
        assert pathlib.Path(file_path)

    def test_get_yaml_plugin(self):
        reader = CdbTxtFiles(self.json_cdb, FileType.YAML)
        file_path=reader.get_plugin_file_path("PluginID2")
        assert pathlib.Path(file_path)

    def test_guessing_cdb_type(self):
        j_reader = CdbTxtFiles.from_folder(self.json_cdb)
        assert j_reader.files_type == FileType.JSON
        y_reader = CdbTxtFiles.from_folder(self.yaml_cdb)
        assert y_reader.files_type == FileType.YAML
