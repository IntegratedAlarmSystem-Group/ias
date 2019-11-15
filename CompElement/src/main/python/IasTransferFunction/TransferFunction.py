import logging

class TransferFunction(object):
    '''
    Base (and abstract) class to provide TFs in python programming language.

    Usage: python TF implementations must extend this class and provide, as a minimum,
    the implementation of the transfer method.

    It is the python equivalent of the JavaTransferExecutor class
    '''
    def __init__(self, asceId, asceRunningId, validityTimeFrame, props):
        '''
        Constructor

        :param asceId: The ID of the ASCE that runs the python TF
        :param asceRunningId: The running ID of the ASCE that runs the python TF
        :param validityTimeFrame: The validity time frame (long)
        :param props: a dictionary of properties
        '''
        assert asceId is not None and asceId!="", "Invalid ID of ASCE"
        self.asceID=asceId
        logging.debug("Building python TF for ASCE %s",self.asceID)
        assert asceRunningId is not None and asceRunningId!="", "Invalid running ID of ASCE"
        self.asceRunningId = asceRunningId
        assert validityTimeFrame>=0, "Invalid validity time frame "+validityTimeFrame
        self.validityTimeFrame=validityTimeFrame
        if props is None:
            self.props = {}
        else:
            assert isinstance(props,dict)
            self.props=props
        self.instance = None
        logging.info("Python TF of %s successfully built",self.asceRunningId)

    def setTemplateInstance(self, instance):
        '''
        Set the instance of the template, if any.

        :param instance: the instance number or None if there is no template
        :return:
        '''
        self.instance=instance
        if (self.instance is None):
            logging.debug("Python TF of %s is NOT templated",self.asceRunningId)
        else:
            logging.info("Python TF of %s has template %d",self.asceRunningId,self.instance)

    def isTemplated(self):
        '''

        :return: the number of the instance or NOne if not template
        '''
        return self.instance is not None

    def shutdown(self):
        '''
        Last method called when the object life terminates.
        It is usually called to free acquired resources.

        :return:
        '''
        pass

    def initialize(self, inputsInfo, outputInfo):
        '''
        Initialize the TF.

        Must be overridden if the user provided implementation needs
        to know the ID and type of the inputs and the output.
        It iusuall implemented to increase the robustness for example
        if the user implemented TF compare the value of the input with a threshold,
        it can be used to check that the input is a numeric type.

        :param inputsInfo: the list of IasioInfo with the ids and type of inputs
        :param outputInfo: the type and ID of the output
        :return: None
        '''
        pass

    def eval(self,compInputs, actualOutput):
        '''
        The eval method to produce the output based on the value of the inputs

        :param compInputs: computing element inputs (IASIOs)
        :param actualOutput: the actual value of the output i.e. tha value computed at previous
                             iteration (IASIO)
        :return: the new output of the ASCE (IASIO)
        '''
        raise NotImplementedError('Python TF implementation missing')