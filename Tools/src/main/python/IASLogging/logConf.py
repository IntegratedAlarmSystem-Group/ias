import datetime
from datetime import timezone
import logging
from pathlib import Path

from IASTools.DefaultPaths import DefaultPaths


class Log():

    # File handler
    file_handler = None

    # Console Handler
    console_handler = None

    # The root logger
    _logger = None

    @classmethod
    def _initLogging (cls, nameFile, file_level_name='info', console_level_name='info') -> logging.Logger:
      '''
      Initialize the logging for IAS environment.

      To get a logger, use getLogger()
      '''

      # Take the path for logs folder
      log_path=Path(DefaultPaths.get_ias_logs_folder())
      log_path.mkdir(parents=True, exist_ok=True)

      cleanedFileName = Path(nameFile).stem

      #Format of the data for the file name
      now = datetime.datetime.now(timezone.utc).strftime('%Y-%m-%dT%H_%M_%S')

      log_levels = {'debug': logging.DEBUG,
                'info': logging.INFO,
                'warn': logging.WARNING,
                'warning': logging.WARNING,
                'error': logging.ERROR,
                'critical': logging.CRITICAL}

      file_level = log_levels.get(file_level_name.lower(), logging.DEBUG)
      console_level = log_levels.get(console_level_name.lower(), logging.INFO)

      file_name=("{0}/{1}_{2}.log".format(str(log_path), cleanedFileName, now))

      print("Python log file name", file_name)

      logger = logging.getLogger(__name__)
      logger.setLevel(logging.DEBUG)

      # Create file handler which logs even debug messages
      cls.file_handler = logging.FileHandler(file_name)
      cls.file_handler.setLevel(file_level)

      # Create console handler with a higher log level
      cls.console_handler = logging.StreamHandler()
      cls.console_handler.setLevel(console_level)

      # Create and add the formatters to the handlers
      formatter_console = logging.Formatter('%(asctime)s%(msecs)d %(levelname)-8s [%(filename)s %(lineno)d] %(message)s' , '%H:%M:%S.')
      formatter_file =  logging.Formatter('%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s')
      cls.file_handler.setFormatter(formatter_file)
      cls.console_handler.setFormatter(formatter_console)

      # Finally, add the handlers to the logger
      if not logger.hasHandlers():
        logger.addHandler(cls.file_handler)
        logger.addHandler(cls.console_handler)

      return logger


    @classmethod
    def getLogger(cls, fileName, fileLevel='info', consoleLevel='info'):
        if cls._logger is None:
            cls._logger = cls._initLogging(fileName, fileLevel, consoleLevel)
        return cls._logger.getChild(fileName)
