"""
Helper for resolving heartbeat configuration from the CDB or defaults.
"""

import os
import logging
from pathlib import Path

from IasCdb.CdbReader import CdbReader

logger = logging.getLogger(__name__)


HB_FREQUENCY_DEFAULT = 5


def get_hb_frequency_from_cdb(cdb_parent_folder: str) -> int:
    """
    Get the HB frequency from the CDB.

    Args:
        cdb_parent_folder: The parent folder of the CDB.

    Returns:
        The HB frequency in seconds read from the IAS CDB.

    Raises:
        ValueError: If the CDB parent folder is not readable or does not exist.
    """
    logger.debug("Getting HB frequency from IAS CDB %s", cdb_parent_folder)
    cdb_path = Path(cdb_parent_folder) / "CDB"
    if not cdb_path.is_dir():
        raise ValueError(f"Invalid CDB folder {cdb_parent_folder}")
    cdb_reader = CdbReader(cdb_parent_folder)
    ias_dao = cdb_reader.get_ias()
    return ias_dao.hb_frequency


def get_hb_frequency(frequency_from_cmdline: int | None, jCdb: str | None) -> int:
    """
    Resolve the HB frequency using a fallback chain:
    1. Frequency passed on the command line
    2. IAS CDB passed on the command line (--jcdb)
    3. IAS CDB from the IAS_CDB environment variable
    4. Default value (5 seconds)

    Args:
        frequency_from_cmdline: Frequency from command line (None if not set).
        jCdb: CDB folder from the command line.

    Returns:
        The frequency in seconds.
    """
    if frequency_from_cmdline is not None:
        logger.debug("Using HB frequency from command line: %d", frequency_from_cmdline)
        return frequency_from_cmdline

    if jCdb is not None:
        logger.debug("Using HB frequency from IAS CDB passed in command line")
        try:
            return get_hb_frequency_from_cdb(jCdb)
        except Exception:
            logger.error("Cannot read HB frequency from CDB folder %s", jCdb)

    if os.getenv("IAS_CDB") is not None:
        logger.debug("Using HB frequency from IAS_CDB environment variable")
        try:
            return get_hb_frequency_from_cdb(os.environ["IAS_CDB"])
        except Exception:
            logger.error("Cannot read HB frequency from IAS_CDB environment")

    logger.info("HB frequency not configured: using default %d", HB_FREQUENCY_DEFAULT)
    return HB_FREQUENCY_DEFAULT
