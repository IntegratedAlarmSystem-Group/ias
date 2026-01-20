#! /usr/bin/env python3
"""
Test the CdbReader.py
"""

from IasCdb.Dao.IasDao import IasDao
from IasCdb.Dao.LogLevelDao import LogLevelDao
from IasCdb.Dao.SoundTypeDao import SoundTypeDao
from IasCdb.Dao.IasTypeDao import IasTypeDao
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

    def test_getIas_yaml(self):
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

    def test_getIasios_json(self):
        reader = CdbReader(self.json_cdb)
        iasios = reader.get_iasios()
        assert iasios is not None
        assert len(iasios) == 16

    def test_getIasios_yaml(self):
        reader = CdbReader(self.yaml_cdb)
        iasios = reader.get_iasios()
        assert iasios is not None
        assert len(iasios) == 18

    def test_getIasio_json(self):
        reader = CdbReader(self.json_cdb)
        assert reader.get_iasio("UNKNOWN-ID") is None
        iasio = reader.get_iasio("OUTPUT-ID")
        assert iasio is not None
        assert iasio.docUrl == "http://wiki.alma.cl/outputID"
        assert iasio.shortDesc == "ID of output"
        assert iasio.iasType == IasTypeDao.ALARM
        assert iasio.id == "OUTPUT-ID"
        assert iasio.emails is None
        assert iasio.sound == SoundTypeDao.NONE
        assert iasio.canShelve == False
        assert iasio.templateId is None
        iasio = reader.get_iasio("SoundInput")
        assert iasio is not None
        assert iasio.id == "SoundInput"
        assert iasio.templateId == "templated-input"
        assert iasio.shortDesc == "Templated input with sound example"
        assert iasio.iasType == IasTypeDao.ALARM
        assert iasio.sound == SoundTypeDao.TYPE2
        assert iasio.canShelve == True
        assert iasio.emails == "stencccr@yujiehanjiao.cc"
        assert iasio.docUrl is None

    def test_getIasio_yaml(self):
        reader = CdbReader(self.yaml_cdb)
        assert reader.get_iasio("UNKNOWN-ID") is None
        iasio = reader.get_iasio("OUTPUT-ID")
        assert iasio is not None
        assert iasio.docUrl == "http://wiki.alma.cl/outputID"
        assert iasio.shortDesc == "ID of output"
        assert iasio.iasType == IasTypeDao.ALARM
        assert iasio.id == "OUTPUT-ID"
        assert iasio.emails is None
        assert iasio.sound == SoundTypeDao.NONE
        assert iasio.canShelve == False
        assert iasio.templateId is None
        iasio = reader.get_iasio("SoundInput")
        assert iasio is not None
        assert iasio.id == "SoundInput"
        assert iasio.templateId == "templated-input"
        assert iasio.shortDesc == "Templated input with sound example"
        assert iasio.iasType == IasTypeDao.ALARM
        assert iasio.sound == SoundTypeDao.TYPE2
        assert iasio.canShelve == True
        assert iasio.emails == "stencccr@yujiehanjiao.cc"
        assert iasio.docUrl is None
