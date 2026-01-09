import os
from IasAlarmGui.config import Config
class TestConfigNoCdb():
    """
    The Config without IAS_CDB being defined in the environment

    Config, in fact, gets the values of the IAS_CDB statically, before instantiating an object.
    """
    @classmethod
    def setup_class(cls):
        cls.old_cdb = os.environ.get('IAS_CDB')
        del os.environ['IAS_CDB']

    @classmethod
    def teardown_class(cls):
        if cls.old_cdb is not None:
            os.environ['IAS_CDB'] = cls.old_cdb

    def test_bsdb_no_cdb(self):

        config = Config(None)

        assert config.get_cdb() is None

        assert config.get_bsdb_url(None, None) is None

        url = "10.15.120.7"
        assert url == config.get_bsdb_url(url)

        default = "127.0.0.1:9099"
        assert default == config.get_bsdb_url(None, default)

        assert config.get_bsdb_url(None) is None

    def test_bsdb_passing_cdb(self):
        """
        The getting the bsdb when a CDB path is given in the constructor"""
        from IasAlarmGui.config import Config
        cdb_path = "src/test"
        config = Config(cdb_path)

        assert cdb_path == config.get_cdb()

        # The url in the CDB (See CDB/ias.yaml)
        url_in_cdb = "192.168.0.196:9095"
        assert url_in_cdb == config.get_bsdb_url(None)

        test_url = "10.192.168.3:909"
        assert test_url == config.get_bsdb_url(test_url)
