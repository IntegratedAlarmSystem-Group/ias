from IasBasicTypes.IasType import IASType


class IasioInfo(object):
    '''
    Python equivalent of the scala IasioInfo
    '''
    def __init__(self, id, iasType):
        '''
        Constructor

        :param id: the not None ID of the IASIO
        :param iasType:  the not None type of the IASIO
        '''
        assert id is not None and id!=""
        self.id=id
        assert iasType is not None and isinstance(iasType,IASType)
        self.iasType=iasType