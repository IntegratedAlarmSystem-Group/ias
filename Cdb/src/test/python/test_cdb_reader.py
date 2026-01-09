#! /usr/bin/env python3
"""
Test the CdbReader.py
"""

from IasCdb.Dao.IasDao import IasDao
from IasCdb.Dao.LogLevelDao import LogLevelDao
from IasCdb.CdbReader import CdbReader
from IasCdb.TextFileType import FileType, TextFileType

class TestCdbReader():

    def setup_method(self):
        self.json_cdb = "src/test/testJsonCdb/"
        self.yaml_cdb = "src/test/testYamlCdb"

    def teardown_method(self):
        pass

    def testFileType(self):
        assert CdbReader(self.json_cdb).cdbTxtFiles.files_type == FileType.JSON
        assert CdbReader(self.yaml_cdb).cdbTxtFiles.files_type == FileType.YAML

    def test_get_ias_json(self):
        reader = CdbReader(self.json_cdb)
        ias = reader.get_ias()
        assert ias is not None
        assert ias.log_level == LogLevelDao.INFO
        assert ias.refresh_rate == 5
        assert ias.validity_threshold == 11
        assert ias.hb_frequency == 10
        assert ias.smtp == 'acaproni:pswd@smtp.test.org'
        assert ias.bsdb_url == '127.0.0.1:9092'

        props = { 'PropName1':'PropValue1', 'PropName2':'PropValue2'}
        assert ias.props == props

    def test_getIas_aml(self):
        reader = CdbReader(self.yaml_cdb)
        ias = reader.get_ias()
        assert ias is not None
        assert ias.log_level == LogLevelDao.INFO
        assert ias.refresh_rate == 5
        assert ias.validity_threshold == 11
        assert ias.hb_frequency == 10
        assert ias.smtp == 'acaproni:pswd@smtp.test.org'
        assert ias.bsdb_url == '127.0.0.1:9092'

        props = { 'P1-Name':'A value for property 1', 'P2-Name':'A value for another property'}
        assert ias.props == props
