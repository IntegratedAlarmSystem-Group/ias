import os
from IASLogging.logConf import Log
from IasAlarmGui.config import Config

class TestConfigWithCdb():
    """
    The Config without IAS_CDB being defined in the environment

    Config, in fact, gets the values of the IAS_CDB statically, before instantiating an object.
    """

     # The url in the CDB (See src/test/CDB/ias.yaml)
    URL_IN_CDB = "192.168.0.196:9095"

    @classmethod
    def setup_class(cls):
        cls.old_cdb = os.environ.get('IAS_CDB')
        os.environ['IAS_CDB'] = "src/test"

    @classmethod
    def teardown_class(cls):
        if cls.old_cdb is not None:
            os.environ['IAS_CDB'] = cls.old_cdb
        else:
            del os.environ['IAS_CDB']

    def test_bsdb_with_cdb(self):
        config = Config(None)

        assert config.get_cdb() is not None

        assert TestConfigWithCdb.URL_IN_CDB == config.get_bsdb_url(None, None)

        url = "10.15.120.7"
        assert url == config.get_bsdb_url(url)

        default = "127.0.0.1:9099"
        assert TestConfigWithCdb.URL_IN_CDB == config.get_bsdb_url(None, default)

        assert TestConfigWithCdb.URL_IN_CDB == config.get_bsdb_url(None)
