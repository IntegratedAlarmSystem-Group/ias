"""
Test the HeartbeatHelp module for resolving HB frequency.
"""

import os

from IasHeartbeat.HeartbeatHelper import (
    get_hb_frequency,
    get_hb_frequency_from_cdb,
    HB_FREQUENCY_DEFAULT,
)


class TestHeartbeatHelp:

    @classmethod
    def setup_class(cls):
        cls.old_cdb = os.environ.get("IAS_CDB")
        if cls.old_cdb is not None:
            del os.environ["IAS_CDB"]

    @classmethod
    def teardown_class(cls):
        if cls.old_cdb is not None:
            os.environ["IAS_CDB"] = cls.old_cdb

    @property
    def _cdb_path(self):
        return "src/test"

    def test_default_value(self):
        assert HB_FREQUENCY_DEFAULT == 5

    def test_cmdline_overrides_cdb(self):
        assert get_hb_frequency(3, self._cdb_path) == 3

    def test_cmdline_zero_disables(self):
        assert get_hb_frequency(0, self._cdb_path) == 0

    def test_cmdline_alone(self):
        assert get_hb_frequency(10, None) == 10

    def test_cdb_from_jcdb(self):
        expected = 7
        assert get_hb_frequency(None, self._cdb_path) == expected

    def test_cdb_from_env(self):
        os.environ["IAS_CDB"] = self._cdb_path
        expected = 7
        assert get_hb_frequency(None, None) == expected
        del os.environ["IAS_CDB"]

    def test_fallback_to_default(self):
        assert get_hb_frequency(None, None) == HB_FREQUENCY_DEFAULT

    def test_invalid_cdb_fallback_to_default(self):
        assert get_hb_frequency(None, "/nonexistent/path") == HB_FREQUENCY_DEFAULT

    def test_get_hb_frequency_from_cdb_direct(self):
        assert get_hb_frequency_from_cdb(self._cdb_path) == 7

    def test_get_hb_frequency_from_cdb_invalid(self):
        try:
            get_hb_frequency_from_cdb("/nonexistent/path")
            assert False, "Expected ValueError"
        except ValueError:
            pass
