from enum import Enum, unique

@unique
class SoundTypeDao(Enum):
    '''
    Valid sound types
    '''
    NONE=1
    TYPE1=2
    TYPE2=3
    TYPE3=4
    TYPE4=5

    @classmethod
    def from_string(cls, snd_type: str):
        if not snd_type:
            raise ValueError("Invalid empty sound type")
        return getattr(SoundTypeDao, snd_type)
    
    def to_string(self) -> str:
        return self.name

    