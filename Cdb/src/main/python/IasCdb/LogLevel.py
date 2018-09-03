from enum import Enum


class LogLevel(Enum):
    '''
    Valid log levels

    See LogLevelDao.java
    '''
    OFF=1
    FATAL=2
    ERROR=3
    WARN=4
    INFO=5
    DEBUG=6
    ALL=7