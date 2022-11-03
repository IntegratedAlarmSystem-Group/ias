import datetime
import errno
import logging
import os


class Log(object):

    # Static variable to know if the loggin has been initialized
    initialized = False

    # File handler
    fileHandler = None

    # Console Handler
    consoleHandler = None

    # The main logger
    logger = None

    @staticmethod
    def initLogging (nameFile,stdoutLevel='info',consoleLevel='info'):
      '''
      Initialize the logging for IAS environment.

      To get a logger, use getLogger()
      '''

      #take the path for logs folder inside $IAS_ROOT
      Log.initialized = True
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
      Log.fileHandler = logging.FileHandler(file)
      Log.fileHandler.setLevel(stdLevel)
      # create console handler with a higher log level
      Log.consoleHandler = logging.StreamHandler()
      Log.consoleHandler.setLevel(consoleLevel)
      # create formatter and add it to the handlers
      formatterConsole = logging.Formatter('%(asctime)s%(msecs)d %(levelname)-8s [%(filename)s %(lineno)d] %(message)s' , '%H:%M:%S.')
      formatterFile =  logging.Formatter('%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s')
      Log.fileHandler.setFormatter(formatterFile)
      Log.consoleHandler.setFormatter(formatterConsole)
      # add the handlers to the logger
      logger.addHandler(Log.fileHandler)
      logger.addHandler(Log.consoleHandler)
      return logger

    @staticmethod
    def getLogger(fileName,stdoutLevel='info',consoleLevel='info'):
        if not Log.initialized:
            Log.logger = Log.initLogging(fileName,stdoutLevel,consoleLevel)
            return Log.logger
        else:
            return Log.logger.getChild(fileName)
