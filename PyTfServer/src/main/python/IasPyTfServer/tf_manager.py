"""Transfer Function (TF) lifecycle manager for the PyTfServer.

This module holds per-ASCE TransferFunction instances and routes
Initialize, Eval, and Shutdown operations based on asceRunningId.
"""

import logging
import importlib

from IasTransferFunction.TransferFunction import TransferFunction

logger = logging.getLogger(__name__)


class TfManager:
    """Manages per-ASCE Python TransferFunction instances.

    Each TF instance is keyed by a unique asceRunningId, allowing the
    server to serve multiple ASCEs concurrently within a single process.

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
        try:
            if asceRunningId in self.tf_instances:
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
        try:
            tf = self.tf_instances.get(asceRunningId)
            if tf is None:
                return (False, f"ASCE {asceRunningId} not initialized", None)
            result = tf.eval(compInputs, actualOutput)
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
        try:
            tf = self.tf_instances.pop(asceRunningId, None)
            if tf is None:
                return False, f"ASCE {asceRunningId} not found"
            tf.shutdown()
            logger.info(f"Shutdown TF for ASCE {asceRunningId}")
            return True, ""
        except Exception as e:
            logger.error(f"Shutdown failed for ASCE {asceRunningId}: {e}")
            return False, str(e)
