"""Transfer Function (TF) lifecycle manager for the PyTfServer.

Holds one ``TransferFunction`` instance per ASCE, keyed by the unique
``asceRunningId``.  The single ``TfManager`` instance lives inside the
``PyTfService`` servicer and is shared across all incoming gRPC calls.

This module does **not** depend on gRPC or protobuf.  It operates purely on
Python IAS types (``IasioInfo``, ``IASIO``) and can be tested in complete
isolation from the network stack.  See ``test_tf_manager.py``.
"""

import logging
import importlib

from IasTransferFunction.TransferFunction import TransferFunction

logger = logging.getLogger(__name__)


class TfManager:
    """Manages per-ASCE Python TransferFunction instances.

    Maintains a ``dict[asceRunningId, TransferFunction]`` that is keyed by
    each ASCE's unique ``asceRunningId``.  Handles dynamic class loading
    from a fully qualified Python class name, instantiation with the
    standard five-argument constructor, and delegation to the TF's own
    ``initialize``, ``eval``, and ``shutdown`` methods.

    Thread-safety: this class is **not** thread-safe at the dictionary
    level; callers (``PyTfService``) must ensure serialized access if
    concurrent Initialize/Shutdown is possible.

    Attributes:
        tf_instances: Mapping from asceRunningId to the active
            TransferFunction object for that ASCE.
    """

    def __init__(self):
        """Initialize an empty TfManager with no TF instances."""
        self.tf_instances: dict[str, TransferFunction] = {}

    def initialize(self, asceRunningId: str, asceId: str, pythonClassName: str,
                   validityTimeFrame: int, props: dict, templateInstance: int | None,
                   inputsInfo: list, outputInfo) -> tuple[bool, str]:
        """Dynamically load and initialize a TransferFunction for an ASCE.

        Imports the TF class from *pythonClassName*, constructs it with the
        provided ASCE parameters, registers it under *asceRunningId*, and
        calls its ``initialize`` method with the I/O metadata.

        Args:
            asceRunningId: Unique identifier for this ASCE instance on the
                server. Must not already be present.
            asceId: Logical name of the ASCE (passed to the TF constructor).
            pythonClassName: Fully qualified Python class name, e.g.
                ``'my_pkg.MyTransferFunction'``.
            validityTimeFrame: Time-frame value for the TF constructor.
            props: Configuration properties passed to the TF constructor.
            templateInstance: Optional template instance index.
            inputsInfo: List of IasioInfo objects describing TF inputs.
            outputInfo: IasioInfo object describing the TF output.

        Returns:
            A ``(success, error)`` tuple. On success *success* is ``True``
            and *error* is an empty string. On failure *success* is ``False``
            and *error* contains a human-readable message.
        """
        logger.debug(f"Initializing TF {pythonClassName} for ASCE {asceRunningId}...")
        try:
            if asceRunningId in self.tf_instances:
                logger.warning(f"TF {pythonClassName} for ASCE {asceRunningId} already initialized: skipping")
                return False, f"ASCE {asceRunningId} already initialized"

            module_part, class_name = pythonClassName.rsplit('.', 1)
            module = importlib.import_module(module_part)
            tf_class = getattr(module, class_name)

            tf_instance = tf_class(asceId, asceRunningId, validityTimeFrame, props, templateInstance)
            tf_instance.initialize(inputsInfo, outputInfo)
            self.tf_instances[asceRunningId] = tf_instance

            logger.info(f"Initialized TF {pythonClassName} for ASCE {asceRunningId}")
            return True, ""
        except Exception as e:
            logger.error(f"Failed to initialize TF for ASCE {asceRunningId}: {e}")
            return False, str(e)

    def eval(self, asceRunningId: str, compInputs: dict, actualOutput) -> tuple:
        """Execute the TransferFunction ``eval`` for the given ASCE.

        Retrieves the TF instance registered under *asceRunningId* and
        delegates to its ``eval`` method.

        Args:
            asceRunningId: The ASCE whose TF should be evaluated.
            compInputs: Mapping of input name to IASIO objects.
            actualOutput: The current IASIO output to carry forward.

        Returns:
            A ``(success, error, result)`` tuple. On success *result* is the
            IASIO returned by the TF. On failure *result* is ``None``.
        """
        logger.debug(f"Executing the eval of the TF of the {asceRunningId} ASCE")
        try:
            tf = self.tf_instances.get(asceRunningId)
            if tf is None:
                logger.error(f"TF of the {asceRunningId} ASCE not initialized: cannto execute eval")
                return (False, f"ASCE {asceRunningId} not initialized", None)
            result = tf.eval(compInputs, actualOutput)
            logger.info(f"TF of {asceRunningId} ASCE executed")
            return (True, "", result)
        except Exception as e:
            logger.error(f"Eval failed for ASCE {asceRunningId}: {e}")
            return (False, str(e), None)

    def shutdown(self, asceRunningId: str) -> tuple[bool, str]:
        """Shut down and remove the TransferFunction for an ASCE.

        Calls the TF's ``shutdown`` method and removes it from the manager.

        Args:
            asceRunningId: The ASCE whose TF should be shut down.

        Returns:
            A ``(success, error)`` tuple. On failure *success`` is ``False``
            if the ASCE was not found.
        """
        logger.debug(f"Shutting down the TF of the {asceRunningId} ASCE")
        try:
            tf = self.tf_instances.pop(asceRunningId, None)
            if tf is None:
                return False, f"ASCE {asceRunningId} not found"
            tf.shutdown()
            logger.info(f"TF for ASCE {asceRunningId} has been shutdown")
            return True, ""
        except Exception as e:
            logger.error(f"Shutdown failed for ASCE {asceRunningId}: {e}")
            return False, str(e)
