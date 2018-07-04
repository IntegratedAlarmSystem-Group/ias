import os
from IASLogging.logConf import Log
import argparse


if __name__=="__main__":
 parser = argparse.ArgumentParser(description='Run iasConverter Script.')
 parser.add_argument(
                        '-ci',
                        '--ConverterId',
                        help='The ID of the supervisor',
                        action='store',
                        required=True)
 parser.add_argument(
                        '-lso',
                        '--levelStdOut',
                        help='Logging level: Set the level of the message for the file logger, default: info level',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)
 parser.add_argument(
                        '-lcon',
                        '--levelConsole',
                        help='Logging level: Set the level of the message for the console logger, default: info level',
                        action='store',
                        choices=['info', 'debug', 'warning', 'error', 'critical'],
                        default='info',
                        required=False)

 parser.add_argument(
                        '-v',
                        '--verbose',
                        help='Increase the verbosity of the output',
                        action='store_true',
                        default=False,
                        required=False)

 parser.add_argument('-o',
		     '--otherArg',
                     nargs='+',
                     help="Insert other argument",
                     required=False)

 parser.add_argument('-j',
		     '--javaArg',
                     nargs='+',
                     help="Insert java argument",
                     required=False)

 args = parser.parse_args()
 id=args.ConverterId
 lso=args.levelStdOut
 lcon=args.levelConsole
 otherArgs=args.otherArg
 javaArgs=args.javaArg

 log = Log()
 logger=log.initLogging(os.path.basename(__file__),lso,lcon)

 oArg=""
 jArg=""
 if otherArgs:
  for elem in otherArgs:
   oArg+=" "+elem
 if javaArgs:
  for elem in javaArgs:
   jArg+=" "+elem


 if not id:
  logger.error("Missing converter ID in command line")
 else:
  logger.info("ID of converter: %s", id)
  LOGID_PARAM="-i "+id

 startScript="iasRun -l s "+LOGID_PARAM+" org.eso.ias.converter.Converter "+id+""+oArg+""+jArg
 logger.info("iasRun -l s %s org.eso.ias.converter.Converter %s %s",LOGID_PARAM,id,oArg,jArg)
 logger.info("Script iasConverter start iasRun script")
 os.system(startScript)
