from enum import Enum


class LogLevel(Enum):
    '''
    Valid log levels

    See LogLevelDao.java
    '''
    OFF=1
    ERROR=2
    WARN=3
    INFO=4
    DEBUG=5
    TRACE=6
    ALL=7
