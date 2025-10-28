#! /usr/bin/env python3
import unittest
import os

from IASLogging.logConf import Log
from IasAlarmGui.config import Config


class TestConfigNoCdb(unittest.TestCase):
    """
    The Config without IAS_CDB being defined in the environment

    Config, in fact, gets the values of the IAS_CDB statically, before instantiating an object.
    """

    def test_BSDB_no_cbd(self):

        config = Config(None)

        self.assertIsNone(config.get_cdb())

        self.assertIsNone(config.get_bsdb_url(None, None))

        url = "10.15.120.7"
        self.assertEqual(url, config.get_bsdb_url(url))

        default = "127.0.0.1:9099"
        self.assertEqual(default, config.get_bsdb_url(None, default))

        self.assertIsNone(config.get_bsdb_url(None))

    def test_BSDBpassing_CDB(self):
        """
        The getting the bsdb when a CDB path is ginven in the constructor"""
        
        cdb_path = "src/test"
        config = Config(cdb_path)

        self.assertEqual(cdb_path, config.get_cdb())

        # The url in the CDB (See CDB/ias.yaml)
        url_in_cdb = "192.168.0.196:9095"
        self.assertEqual(url_in_cdb, config.get_bsdb_url(None))

        test_url = "10.192.168.3:909"
        self.assertEqual(test_url, config.get_bsdb_url(test_url))

if __name__ == '__main__':
    logger=Log.getLogger(__file__)
    logger.info("Start main")
    unittest.main()