import logging
import sys
import os, errno
import datetime


class Log(object):
  @staticmethod
  def GetLoggerFile(fileName):
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
#    fileNameN='IasRoot'+now
    fileNameN=fileName+now
    #path of the file
    file=("{0}/logs/{1}.log".format(logPath, fileNameN))

    # set up logging to file - see previous section for more details
    logging.basicConfig(level=logging.DEBUG,
                        format='%(asctime)s%(msecs)d  | %(levelname)s | [%(filename)s %(lineno)d] [%(threadName)s] | %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S.',
                        filename=file)
    # define a Handler which writes INFO messages or higher to the sys.stderr
    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
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
