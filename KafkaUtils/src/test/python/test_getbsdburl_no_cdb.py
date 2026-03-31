import os

from IasLogging.log import Log
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
class TestGetBsdbUrlNoCdb():
    """
    Test the getting of the BSDB URL without IAS_CDB being defined in the environment
    """
    @classmethod
    def setup_class(cls):
        Log.init_logging(__file__)
        cls.old_cdb = os.environ.get('IAS_CDB')
        if cls.old_cdb is not None:
            del os.environ['IAS_CDB']

    @classmethod
    def teardown_class(cls):
        if cls.old_cdb is not None:
            os.environ['IAS_CDB'] = cls.old_cdb

    def test_bsdb_no_cdb(self):

        assert IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS==IasKafkaHelper.get_bsdb_url(kafka_brokers=None, jCdb=None)

        url = "10.15.120.7"
        assert url == IasKafkaHelper.get_bsdb_url(kafka_brokers=url, jCdb=None)

    def test_bsdb_passing_cdb(self):
        """
        The getting the bsdb when a CDB path is given in the constructor"""
        cdb_path = "src/test"

        # The url in the CDB (See CDB/ias.yaml)
        url_in_cdb = "192.168.0.196:9095"
        assert url_in_cdb == IasKafkaHelper.get_bsdb_url(kafka_brokers=None, jCdb=cdb_path)

        test_url = "10.192.168.3:909"
        assert test_url == IasKafkaHelper.get_bsdb_url(kafka_brokers=test_url, jCdb=cdb_path)
