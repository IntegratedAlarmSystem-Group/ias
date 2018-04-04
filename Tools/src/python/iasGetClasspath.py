#! /usr/bin/env python
'''
Writes the classpath in the stdout

@author: acaproni
'''

from IASTools.CommonDefs import CommonDefs
from logConf import Log
if __name__ == '__main__':
    log=Log()
    logger=log.GetLoggerFile(os.path.basename(__file__).split(".")[0])
    logger.info(CommonDefs.buildClasspath())
