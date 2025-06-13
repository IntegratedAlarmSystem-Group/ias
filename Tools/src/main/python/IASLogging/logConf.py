import datetime, timezone
import errno
import logging
import os

from IASTools.DefaultPaths import DefaultPaths


class Log(object):

    # Static variable to know if the logging has been initialized
    initialized = False

    # File handler
    file_handler = None

    # Console Handler
    console_handler = None

    # The main logger
    logger = None


    @staticmethod
    def initLogging (nameFile, file_level_name='info', console_level_name='info'):
      '''
      Initialize the logging for IAS environment.

      To get a logger, use getLogger()
      '''

      # Take the path for logs folder
      Log.initialized = True
      logPath=DefaultPaths.get_ias_logs_folder()

      #If the file doesn't exist it's created
      try:
          os.makedirs(logPath)
      except OSError as e:
          if e.errno != errno.EEXIST:
           raise

      cleanedFileName = nameFile.split(os.sep)[len(nameFile.split(os.sep))-1]
      #Format of the data for the file name
      now = datetime.datetime.now(timezone.utc).strftime('%Y-%m-%dT%H_%M_%S')[:-3]

      log_levels = {'debug': logging.DEBUG,
                'info': logging.INFO,
                'warn': logging.WARNING,
                'warning': logging.WARNING,
                'error': logging.ERROR,
                'critical': logging.CRITICAL}

      file_level = log_levels.get(file_level_name.lower(), logging.NOTSET)
      console_level = log_levels.get(console_level_name.lower(), logging.NOTSET)

      file_name=("{0}/{1}.{2}.log".format(logPath, cleanedFileName, now))

      print("Pyhton log file name", file_name)

      logger = logging.getLogger(__name__)
      logger.setLevel(logging.DEBUG)

      # Create file handler which logs even debug messages
      Log.file_handler = logging.FileHandler(file_name)
      Log.file_handler.setLevel(file_level)

      # Create console handler with a higher log level
      Log.console_handler = logging.StreamHandler()
      Log.console_handler.setLevel(console_level)

      # Create and add the formatters to the handlers
      formatter_console = logging.Formatter('%(asctime)s%(msecs)d %(levelname)-8s [%(filename)s %(lineno)d] %(message)s' , '%H:%M:%S.')
      formatter_file =  logging.Formatter('%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s')
      Log.file_handler.setFormatter(formatter_file)
      Log.console_handler.setFormatter(formatter_console)

      # Finally, add the handlers to the logger
      logger.addHandler(Log.file_handler)
      logger.addHandler(Log.console_handler)

      return logger


    @staticmethod
    def getLogger(fileName, fileLevel='info', consoleLevel='info'):
        if not Log.initialized:
            Log.logger = Log.initLogging(fileName, fileLevel, consoleLevel)
            return Log.logger
        else:
            return Log.logger.getChild(fileName)
