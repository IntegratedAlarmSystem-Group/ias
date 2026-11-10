from IasTransferFunction.TransferFunction import TransferFunction


class PassThroughTF(TransferFunction):
    def eval(self, compInputs, actualOutput):
        val = compInputs.get("in1")
        if val is not None:
            return actualOutput.updateValue(val.value)
        return actualOutput


class DoublingTF(TransferFunction):
    def eval(self, compInputs, actualOutput):
        val = compInputs.get("in1")
        if val is not None:
            return actualOutput.updateValue(val.value * 2)
        return actualOutput
