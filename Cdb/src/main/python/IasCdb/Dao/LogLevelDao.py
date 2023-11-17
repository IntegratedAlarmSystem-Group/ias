from enum import Enum, unique

@unique
class LogLevelDao(Enum):
    '''
    Valid log levels
    '''
    OFF=1
    ERROR=2
    WARN=3
    INFO=4
    DEBUG=5
    TRACE=6
    ALL=7

    @classmethod
    def from_string(cls, level: str):
        if not level:
            raise ValueError("Invalid empty log level")
        return getattr(LogLevelDao, level)
    
    def to_string(self) -> str:
        return self.name
