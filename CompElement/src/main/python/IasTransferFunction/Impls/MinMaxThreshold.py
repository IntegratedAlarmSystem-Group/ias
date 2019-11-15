import logging
import sys

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.IasType import IASType
from IasTransferFunction.TransferFunction import TransferFunction


class MinMaxThreshold(TransferFunction):
    '''
    The python TF implementation of the min max threshold

    This TF is used mostly for testing python TF and to provide an example.
    In operation the scala version should be preferred because is more performant
    '''

    def __init__(self, asceId, asceRunningId, validityTimeFrame, props):
        '''
        Constructor

        :param asceId:
        :param asceRunningId:
        :param validityTimeFrame:
        :param props:
        '''
        super().__init__(asceId, asceRunningId,validityTimeFrame, props)
        logging.info("MinMaxThreshold built for ASCE %s",self.asceRunningId)

        # The ID of the input
        self.idOfInput = None

        # The names of the properties to set the threshold to activate the output
        # including histeresys
        self.highOnPropName = 'HighOn'
        self.highOffPropName = 'HighOff'
        self.lowOnPropName = 'LowOn'
        self.lowOffPropName = 'LowOff'

        # the name of the property to set the priority of the Alarm set when
        # the value of the input passes the given thresholds
        self.priorityPropName = 'MEDIUM'

        # Get thresholds from props is defined
        if props is None:
            props = {}
        highOnFromProps=props.get(self.highOnPropName)
        highOffFromProps=props.get(self.highOffPropName)
        lowOnFromProps=props.get(self.lowOnPropName)
        lowOffFromProps=props.get(self.lowOffPropName)

        if highOnFromProps is not None:
            self.highOn = float(highOnFromProps)
        else:
            self.highOn=sys.maxsize

        if highOffFromProps is not None:
            self.highOff = float(highOffFromProps)
        else:
            self.highOff=sys.maxsize

        if lowOnFromProps is not None:
            self.lowOn = float(lowOnFromProps)
        else:
            self.lowOn=-sys.maxsize

        if lowOffFromProps  is not None:
            self.lowOff = float(lowOffFromProps)
        else:
            self.lowOff=-sys.maxsize

        priorityStr = props.get(self.priorityPropName)
        if priorityStr is not None:
            self.alarmSet = Alarm.fromString(priorityStr)
        else:
            self.alarmSet = Alarm.SET_MEDIUM

    def initialize(self, inputsInfo, outputInfo):
        '''
        Initializiation

        :param inputsInfo: The list of IDs and types of the inputs
        :param outputInfo: The ID and type of the output
        :return:
        '''
        logging.debug("Initializing MinMaxThreshold python TF of %s",self.asceRunningId)

        assert len(inputsInfo)==1, \
            'Wrong number of inputs of MinMaxThreshold of ASCE %: % instead of 1' % (self.asceRunningId,len(inputsInfo))

        inputType = inputsInfo[0].iasType
        self.idOfInput = inputsInfo[0].id
        logging.info('Input of MinMaxThreshold of ASCE %s: %s',self.asceRunningId,self.idOfInput)
        assert  inputType==IASType.DOUBLE or \
                 inputType==IASType.FLOAT or \
                 inputType==IASType.BYTE or \
                 inputType==IASType.INT or \
                 inputType==IASType.LONG or \
                 inputType==IASType.SHORT, \
                 "Not numeric type of MinMaxThreshold of ASCE %: %" % (self.asceRunningId,inputType)

        assert outputInfo.iasType==IASType.ALARM, \
            "Output type of is MinMaxThreshold of ASCE % is % instead of ALARM" % (self.asceRunningId,outputInfo.iasType)

    def eval(self,compInputs, actualOutput):
        '''
        Run the TF

        :param compInputs: The inputs (Map of IASIOs with their IDs as keys)
        :param actualOutput: the actual output (IASIO)
        :return: the new output of the ASCE (IASIO)
        '''
        logging.debug("Running python MinMaxThreshold TF of ASCE %s",self.asceRunningId)

        inputValue = compInputs[self.idOfInput]

        props = actualOutput.props
        props["actualValue"]=inputValue.value

        wasActivated = actualOutput.value is not None and actualOutput.value.isSet

        condition =  inputValue.value >= self.highOn or \
                     inputValue.value <= self.lowOn or \
                     wasActivated and (inputValue.value>=self.highOff or inputValue.value<=self.lowOff)

        newValue = None
        if condition:
            newValue = self.alarmSet
        else:
            newValue = Alarm.CLEARED

        return actualOutput.updateProps(props).updateValue(newValue)





