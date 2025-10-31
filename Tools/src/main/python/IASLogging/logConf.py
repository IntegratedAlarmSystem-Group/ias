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

    @staticmethod
    def getLevel(level_name: str) -> int:
        '''
        Get the logging level from its name
        '''
        log_levels = {'debug': logging.DEBUG,
                      'info': logging.INFO,
                      'warn': logging.WARNING,
                      'warning': logging.WARNING,
                      'error': logging.ERROR,
                      'critical': logging.CRITICAL}

        return log_levels.get(level_name.lower(), logging.INFO)

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

      file_level = cls.getLevel(file_level_name)
      console_level = cls.getLevel(console_level_name)

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
        """
        Get a logger instance for the given file name.

        The logger is instantiated only once setting the passed levels
        in the console and in the file handlers.

        Subsequent call to getLogger do not change the logging levels of the handlers.
        
        To customize the level of the logger, the user must set the log level in the logger
        returned by this method.

        Note that in python logging:
        - The logger’s level acts as a global filter.
        - If a log message is below the logger’s level, it will be ignored entirely, 
          regardless of the handler’s level.
        Example: If the logger is set to WARNING, then DEBUG and INFO messages will be discarded
                 before they even reach any handler.

        This means that if the user want to customize the logging for a specific file, the first call to getLogger
        must set the log levels of the handlers at the lowest level and customize the level of the logger returned 
        by this method. Setting the level of the logger affects the messages passed to both handlers.

        TODO: the API of getLogger is ambigous because apart of the first call, the levels passed to the function are ignored.
              We should probably split intialization and and getLogger in 2 steps requested by the user

        Params:
            fileName (str): The name of the file for which the logger is requested.
            fileLevel (str): The logging level for the file handler (default is 'info').
            consoleLevel (str): The logging level for the console handler (default is 'info').
        """
        if cls._logger is None:
            cls._logger = cls._initLogging(fileName, fileLevel, consoleLevel)
        return cls._logger.getChild(Path(fileName).stem)
