import logging
import sys
import os, errno
import datetime


class Log(object):
  @staticmethod
  def initLogging (nameFile,stdoutLevel='info',consoleLevel='info'):
    #take the path for logs folder inside $IAS_ROOT

    logPath=os.environ["IAS_LOGS_FOLDER"]
    #If the file doesn't exist it's created
    try:
        os.makedirs(logPath)
    except OSError as e:
        if e.errno != errno.EEXIST:
         raise
    cleanedFileName = nameFile.split(os.sep)[len(nameFile.split(os.sep))-1]
    #Format of the data for filename
    now = datetime.datetime.utcnow().strftime('%Y-%m-%d_%H:%M:%S.%f')[:-3]
    LEVELS = { 'debug':logging.DEBUG,
            'info':logging.INFO,
            'warning':logging.WARNING,
            'error':logging.ERROR,
            'critical':logging.CRITICAL,
            }
    stdLevel_name = stdoutLevel
    consoleLevel= consoleLevel
    stdLevel = LEVELS.get(stdLevel_name, logging.NOTSET)
    consoleLevel = LEVELS.get(consoleLevel, logging.NOTSET)
    file=("{0}/{1}.log".format(logPath, str(cleanedFileName)+str(now)))

    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)
    # create file handler which logs even debug messages
    fh = logging.FileHandler(file)
    fh.setLevel(stdLevel)
    # create console handler with a higher log level
    ch = logging.StreamHandler()
    ch.setLevel(consoleLevel)
    # create formatter and add it to the handlers
    formatterConsole = logging.Formatter('%(asctime)s%(msecs)d %(levelname)-8s [%(filename)s %(lineno)d] %(message)s' , '%H:%M:%S.')
    formatterFile =  logging.Formatter('%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s')
    fh.setFormatter(formatterFile)
    ch.setFormatter(formatterConsole)
    # add the handlers to the logger
    logger.addHandler(fh)
    logger.addHandler(ch)
    return logger
