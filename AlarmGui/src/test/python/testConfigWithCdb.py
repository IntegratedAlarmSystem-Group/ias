#! /usr/bin/env python3
import unittest
import os

from IASLogging.logConf import Log
from IasAlarmGui.config import Config


class TestConfigWithCdb(unittest.TestCase):
    """
    The Config without IAS_CDB being defined in the environment

    Config, in fact, gets the values of the IAS_CDB statically, before instantiating an object.
    """

     # The url in the CDB (See src/test/CDB/ias.yaml)
    URL_IN_CDB = "192.168.0.196:9095"

    def test_BSDB_no_cbd(self):

        config = Config(None)

        self.assertIsNotNone(config.get_cdb())

        self.assertEqual(TestConfigWithCdb.URL_IN_CDB, config.get_bsdb_url(None, None))

        url = "10.15.120.7"
        self.assertEqual(url, config.get_bsdb_url(url))

        default = "127.0.0.1:9099"
        self.assertEqual(TestConfigWithCdb.URL_IN_CDB, config.get_bsdb_url(None, default))

        self.assertEqual(TestConfigWithCdb.URL_IN_CDB, config.get_bsdb_url(None))

if __name__ == '__main__':
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()