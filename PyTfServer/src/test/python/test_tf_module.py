"""Mock Python TransferFunction implementations for unit testing.

These are **not** production TFs. They provide simple, deterministic
behaviour so that ``test_tf_manager.py`` and ``test_server.py`` can
verify the full Initialize/Eval/Shutdown lifecycle without external
dependencies.

Neither class requires the gRPC server.  They can be loaded directly by
``TfManager.initialize`` via ``importlib`` using their fully qualified
name (``test_tf_module.PassThroughTF`` etc.).
"""

from IasTransferFunction.TransferFunction import TransferFunction


class PassThroughTF(TransferFunction):
    """Simple TF that passes the ``in1`` value through unchanged.

    Used by tests to verify that the ``eval`` path carries data end-to-end
    without transformation.
    """
    def eval(self, compInputs, actualOutput):
        val = compInputs.get("in1")
        if val is not None:
            return actualOutput.updateValue(val.value)
        return actualOutput


class DoublingTF(TransferFunction):
    """TF that doubles the ``in1`` value.

    Used by tests to confirm that ``eval`` actually invokes the TF's
    logic (output value == 2 * input value).
    """
    def eval(self, compInputs, actualOutput):
        val = compInputs.get("in1")
        if val is not None:
            return actualOutput.updateValue(val.value * 2)
        return actualOutput


class TriplingTF(TransferFunction):
    """TF that triples the ``in1`` value.

    Used by tests to verify that the server correctly routes Eval
    requests to the proper TF instance when multiple ASCEs are
    active simultaneously (output value == 3 * input value).
    """
    def eval(self, compInputs, actualOutput):
        val = compInputs.get("in1")
        if val is not None:
            return actualOutput.updateValue(val.value * 3)
        return actualOutput
