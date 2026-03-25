import os
from IasLogging.log import Log
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

class TestGetBsdbUrlWithCdb():
    """
    The getting the BSDB URL without IAS_CDB being defined in the environment
    """

     # The url in the CDB (See src/test/CDB/ias.yaml)
    URL_IN_CDB = "192.168.0.196:9095"

    @classmethod
    def setup_class(cls):
        Log.init_logging(__file__)
        cls.old_cdb = os.environ.get('IAS_CDB')
        os.environ['IAS_CDB'] = "src/test"

    @classmethod
    def teardown_class(cls):
        if cls.old_cdb is not None:
            os.environ['IAS_CDB'] = cls.old_cdb
        else:
            del os.environ['IAS_CDB']

    def test_bsdb_with_cdb(self):

        assert TestGetBsdbUrlWithCdb.URL_IN_CDB == IasKafkaHelper.get_bsdb_url(None, None)

        url = "10.15.120.7:10015"
        assert url == IasKafkaHelper.get_bsdb_url(kafka_brokers=url, jCdb=None)

        default = "127.0.0.1:9099"
        assert TestGetBsdbUrlWithCdb.URL_IN_CDB == IasKafkaHelper.get_bsdb_url(kafka_brokers=None, jCdb="src/test")
