from IasBasicTypes import OperationalMode


class IASIO(object):
    '''
    The IASIO object passed to python TFs.
    It is the python implementation of the java IasIOJ.

    The java and scala IASIOs passed to TFs are immutable but immutability
    is not provided for python TFs because there is no python native (clean) way to
    define immutable objects
    '''
    def __init__(self,
                 id,
                 runningId,
                 mode, # OperationalMode
                 iasType, # IasType
                 validity, #Validity
                 value,
                 productionTStamp, # LONG
                 props): # Dictionary of properties
        '''
        Constructor

        :param id: the ID of the IASIO
        :param runningId:  the running ID of the IASIO
        :param mode: OperationalMode
        :param iasType: IasType
        :param validity: Validity
        :param value: the value
        :param productionTStamp: the timestamp whan it has been produced
        :param props: a dictionary of properties
        '''
        assert id is not None, "Invalid ID"
        self.id=id
        assert runningId is not None, "Inavalid running ID"
        self.runningId=runningId
        self.mode=mode
        self.iasType=iasType
        self.validity=validity
        self.value=value
        self.productionTStamp=productionTStamp
        if props is None:
            self.props={}
        else:
            self.props=props

    def updateMode(self, newMode):
        '''

        :param newMode: The new mode
        :return: A new IASIO with the mode updated
        '''
        assert isinstance(newMode, OperationalMode), "Invalid type of operational mode"
        return IASIO(self.id,
                      self.runningId,
                      newMode,
                      self.iasType,
                      self.validity,
                      self.value,
                      self.props)

    def updateValue(self, newValue):
        '''

        :param newValue: The new value
        :return: A new IASIO with the value updated
        '''
        return IASIO(self.id,
                     self.runningId,
                     self.mode,
                     self.iasType,
                     self.validity,
                     newValue,
                     self.props)

    def updateProps(self, newProps):
        '''

        :param newProps: the new properties (dictionary)
        :return: A new IASIO with the properties updated
        '''
        if newProps is None:
            newProps = {}
        return IASIO(self.id,
                     self.runningId,
                     self.mode,
                     self.iasType,
                     self.validity,
                     self.value,
                     newProps)