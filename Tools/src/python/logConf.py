import logging
import sys
import os, errno


class Log():

    def GetLogger(fileName):
        LEVELS = {'debug': logging.DEBUG,'info': logging.INFO,'warning': logging.WARNING,'error': logging.ERROR,'critical': logging.CRITICAL}

        if len(sys.argv) > 1:
            level_name = sys.argv[1]
            level = LEVELS.get(level_name, logging.NOTSET)
            logging.basicConfig(level=level)

        #Define logger with logging import
        logger = logging.getLogger()
        #Set the level of the message visualize
        logger.setLevel(logging.DEBUG)

        #Set the format of the log
        logFormatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")
        #Set path where save the file and the name of the file.
        logPath="../IAS_LOGS_FOLDER"
        try:
            os.makedirs(logPath)
        except OSError as e:
            if e.errno != errno.EEXIST:
                raise
        fileHandler = logging.FileHandler("{0}/{1}.log".format(logPath, fileName))
        fileHandler.setFormatter(logFormatter)
        logger.addHandler(fileHandler)
        #Start stream for write into file, from here when it's insert the logger. write all into file.
        consoleHandler = logging.StreamHandler()
        consoleHandler.setFormatter(logFormatter)
        logger.addHandler(consoleHandler)
        return logger