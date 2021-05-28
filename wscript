from waflib import Logs

top = '.'
out = 'build'

# IAS modules to build
IasModules = [
    'Tools',
    'Cdb',
    'BasicTypes',
    'CdbChecker',
    'KafkaUtils',
    'CommandsAndReplies',
    'Heartbeat',
    'Plugin',
    'PythonPluginFeeder',
    'Converter',
    'CompElement',
    'DistributedUnit',
    'Supervisor',
    'WebServerSender',
    'TransferFunctions',
    'SinkClient',
    'Extras',
    'Monitor']

import os

'''
    Since release 12, the IAS is built using the Waf build tool.
    The description of the build procedure is in WafBuildTools/README.md
'''

def options(ctx):
    pass

def build(bld):
    bld.env.PREFIX = os.environ['IAS_ROOT']
    Logs.info("IAS: Building in %s",bld.path.abspath())
    Logs.info('IAS: build command %s',bld.cmd)
    Logs.info('IAS: IAS PREFIX %s', bld.env.PREFIX)

    if bld.cmd == 'install':
        from IasWafBuildTools.Installer import Installer
        installer = Installer(bld, bld.bldnode, os.environ['IAS_ROOT'])
        bld.add_to_group(installer)
    else:
        bld.recurse(IasModules)
    #elif bld.cmd == 'clean':
    #    Logs.info("IAS: cleaning")
    #    bld.recurse(IasModules)
    #    bld(rule='rm -rf build/*', always=True)
    #    Logs.info("IAS: cleaned")
    #else:
    #    Logs.info("IAS: running command %s", bld.cmd)
    #bld.recurse(IasModules)

def configure(conf):
    Logs.info ("IAS: configure in %s",conf.path.abspath())
    conf.load('IasWafBuildTools', tooldir='WafBuildTools/src/main/python/')
    from IasWafBuildTools.IntegrityCheck import checkIntegrity
    checkIntegrity(conf)

    conf.recurse(IasModules)

