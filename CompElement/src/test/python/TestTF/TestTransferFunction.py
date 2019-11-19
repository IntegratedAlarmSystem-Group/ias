import logging

from IasBasicTypes.OperationalMode import OperationalMode
from IasTransferFunction.TransferFunction import TransferFunction


class TestTransferFunction(TransferFunction):
    """
    A transfer function for test:
      - it gets one input
      - sets the output to the same value of the input
      - sets the mode of the input to the output
      - sets the properties (passed in the constructor) to the output

    This TF is used for testing all possible type conversions of inputs and
    output
    """

    def __init__(self, asceId, asceRunningId, validityTimeFrame, props):
        '''
        Constructor

        :param asceId:
        :param asceRunningId:
        :param validityTimeFrame:
        :param props:
        '''
        super().__init__(asceId, asceRunningId,validityTimeFrame, props)
        logging.info("TestTransferFunction built for ASCE %s",self.asceRunningId)

        if props is not None:
            self.props = props
        else:
            self.props = {}

    def initialize(self, inputsInfo, outputInfo):
        '''
        Initializiation

        :param inputsInfo: The list of IDs and types of the inputs
        :param outputInfo: The ID and type of the output
        :return:
        '''
        logging.debug("Initializing TestTransferFunction python TF of %s",self.asceRunningId)

        self.inputType = inputsInfo[0].iasType
        self.idOfInput = inputsInfo[0].id

    def eval(self,compInputs, actualOutput):
        '''
        Run the TF

        :param compInputs: The inputs (Map of IASIOs with their IDs as keys)
        :param actualOutput: the actual output (IASIO)
        :return: the new output of the ASCE (IASIO)
        '''
        logging.debug("Running python MinMaxThreshold TF of ASCE %s",self.asceRunningId)

        inputIASIO = compInputs[self.idOfInput]
        inputValue = inputIASIO.value
        inputMode = OperationalMode.fromString(inputIASIO.mode)

        print("**** value",inputValue,"**** mode",inputMode,"type",self.inputType)

        if type(inputValue)==str:
            inputValue =self.inputType.convertStrToValue(inputValue)

        return actualOutput.updateProps(inputIASIO.props).updateMode(inputMode).updateValue(inputValue)