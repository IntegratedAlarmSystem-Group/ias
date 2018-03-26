from logConf import Log

def testPrint():
  logger=Log.GetLoggerFile(__name__)
  logger.info("this is a test")
