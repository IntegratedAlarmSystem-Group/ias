from enum import Enum, unique

@unique
class IasTypeDao(Enum):
    '''
    Valid IASIO types
    '''
    LONG=1
    INT=2 
    SHORT=3
    BYTE=4
    DOUBLE=5
    FLOAT=6
    BOOLEAN=7
    CHAR=8
    STRING=9
    TIMESTAMP=10
    ARRAYOFDOUBLES=11
    ARRAYOFLONGS=12
    ALARM=13

    @classmethod
    def from_string(cls, ias_type: str):
        if not ias_type:
            raise ValueError("Invalid empty IAS type")
        return getattr(IasTypeDao, ias_type)
    
    def to_string(self) -> str:
        return self.name
