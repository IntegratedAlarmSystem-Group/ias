import datetime
from datetime import timezone
from email import parser
import logging
from pathlib import Path
from threading import Lock
from logging.handlers import RotatingFileHandler
import argparse

from IasTools.DefaultPaths import DefaultPaths


class Log():
    """
    Log provides common initialization for all python logging in IAS environment. 
    It defines a file handler and a console handler, with the different format and levels.
    
    The log file is created in the logs folder with the name of the script and the date and time of the execution. 
    To get a logger, use standard logging.getLogger().
    """

    # File handler
    _file_handler = None

    # Console Handler
    _console_handler = None

    # The root logger
    # initLogging creates the root logger and initializes the handlers, levels and so on.
    # It the logging has bene initialized, _root_logger is not None
    _root_logger: logging.Logger|None = None

    # The mutex to protect the initialization
    _init_lock = Lock()

    @staticmethod
    def get_level(level_name: str) -> int:
        '''
        Get the logging level from its name
        '''
        if not level_name:
            raise ValueError("The log level name cannot be empty")
        log_levels = {'debug': logging.DEBUG,
                      'info': logging.INFO,
                      'warn': logging.WARNING,
                      'warning': logging.WARNING,
                      'error': logging.ERROR,
                      'critical': logging.CRITICAL}

        return log_levels.get(level_name.lower(), logging.INFO)
    
    @classmethod 
    def get_file(cls, name_file: str) -> Path:
        """
        Return the full path of the log file for a given nameFile.

        :param name_file: The name of the log file with or without .log extension.
        :return: The full path of the log file with the extension added if missing 
        """
        if not name_file:
            raise ValueError("The name of the log cannot be empty")
        if name_file.lower().endswith('.log'):
            name_file = name_file[:-4]
        log_path=Path(DefaultPaths.get_ias_logs_folder())
        log_path.mkdir(parents=True, exist_ok=True)
        cleanedFileName = Path(name_file).stem
        now = datetime.datetime.now(timezone.utc).strftime('%Y-%m-%dT%H_%M_%S')
        return log_path / f"{cleanedFileName}_{now}.log"

    @classmethod
    def init_logging (cls, name_file, file_level_name='info', console_level_name='info') -> None:
      '''
      Initialize the logging for IAS environment.

      To get a logger, use getLogger()
      '''
      with cls._init_lock:
        if cls._root_logger is not None:
            cls._root_logger.warning("Logging already initialized. Ignoring the call to initLogging")
            return
        
        file_level = cls.get_level(file_level_name)
        console_level = cls.get_level(console_level_name)

        cls._root_logger = logging.getLogger() # Creates the root logger (the one with no name)
        cls._root_logger.setLevel(logging.DEBUG) # First filter otherwise not all the logs arrive to the handlers

        # Set the formatter for the console handler
        cls._console_handler = logging.StreamHandler()
        formatter_console = logging.Formatter('%(asctime)s%(msecs)d %(levelname)-8s [%(filename)s %(lineno)d] %(message)s' , '%H:%M:%S.')
        cls._console_handler.setFormatter(formatter_console)
        cls._console_handler.setLevel(console_level)
        cls._root_logger.addHandler(cls._console_handler)
        cls._root_logger.info("Log Console level: %s", console_level_name)

        # Create file handler which logs even debug messages
        # Take the path for logs folder
        log_path=Path(DefaultPaths.get_ias_logs_folder())
        log_path.mkdir(parents=True, exist_ok=True)
        file_name = cls.get_file(name_file)
        try:
            cls._file_handler = RotatingFileHandler(
                file_name,
                maxBytes=50 * 1024 * 1024,  # 50 MB
                backupCount=5,
                encoding="utf-8")
            cls._file_handler.setLevel(file_level)
            formatter_file =  logging.Formatter('%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s')
            cls._file_handler.setFormatter(formatter_file)
            cls._root_logger.addHandler(cls._file_handler)  
            cls._root_logger.info("Log file: %s, file level: %s", file_name, file_level_name)
        except Exception as e:
            cls._root_logger.error("Error creating file handler for log file %s: %s", file_name, e)
            cls._file_handler = None

        # Avoid noisy stderr stack traces on handler write errors
        logging.raiseExceptions = False

    @classmethod
    def add_log_arguments_to_parser(cls, parser: argparse.ArgumentParser) -> None:
        '''
        Add the log arguments to an argparse parser.

        This function is meant to ease the addition of log arguments to all the scripts in IAS environment,
        to have a consistent way to configure logging.

        :param parser: The argparse parser to which add the log arguments.
        '''
        if not parser:
            raise ValueError("The parser cannot be None")
        parser.add_argument(
                        '-lf',
                        '--logLevelFile',
                        help='Logging level: Set the level of the message for the file logger (default: info)',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)
        parser.add_argument(
                        '-lc',
                        '--logLevelConsole',
                        help='Logging level: Set the level of the message for the console logger, (default: info)',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)
        
    @classmethod
    def init_log_from_cmdline_args(cls, args: argparse.Namespace, name_file: str) -> None:
        '''
        Initialize the logging from the command line arguments.

        The arguments in the command line must be proveded as defined by :meth:`~Log.add_log_arguments_to_parser`.

        This function is meant to ease the initialization of logging from command line arguments in all the scripts in IAS environment,
        to have a consistent way to configure logging.

        init_log_from_cmdline_args delegates to :meth:`~Log.init_logging`.

        :param args: The command line arguments as parsed by argparse.
        :param name_file: The name of the log file with or without .log extension.
        '''
        if not args:
            raise ValueError("The args cannot be None")
        cls.init_logging(name_file, args.logLevelFile, args.logLevelConsole)

    @classmethod
    def is_initialized(cls) -> bool:
        '''
        Check if the logging has been initialized.

        :return: True if the logging has been initialized, False otherwise.
        '''
        with cls._init_lock:
            return cls._root_logger is not None
