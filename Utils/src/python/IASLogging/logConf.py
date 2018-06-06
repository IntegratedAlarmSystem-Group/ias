import logging
import sys
import os, errno
import datetime


class Log():
  @staticmethod
  def initLogging (nameFile,stdoutLevel='info',consoleLevel='info'):
    #take the path for logs folder inside $IAS_ROOT
    logPath=os.environ["IAS_ROOT"]
    #If the file doesn't exist it's created
    try:
        os.makedirs(logPath+"/logs")
    except OSError as e:
        if e.errno != errno.EEXIST:
         raise

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
    file=("{0}/logs/{1}.log".format(logPath, str(nameFile)+str(now)))


    logging.basicConfig(level=stdLevel,format='%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S.', filename=file)
    #path of the file


    # set up logging to file - see previous section for more details

    # define a Handler which writes INFO messages or higher to the sys.stderr
    console = logging.StreamHandler()
    console.setLevel(consoleLevel)
    # set a format which is simpler for console use
    formatter = logging.Formatter('%(asctime)s%(msecs)d %(levelname)-8s [%(filename)s %(lineno)d] %(message)s' , '%H:%M:%S.')
    # tell the handler to use this format
    console.setFormatter(formatter)
    # add the handler to the root logger
    logging.getLogger('').addHandler(console)

    # Now, define a couple of other loggers which might represent areas in your
    # application:

    logger1 = logging.getLogger(__name__)

    return logger1

