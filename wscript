from waflib import Logs

top = '.'
out = 'build'

# IAS modules to build
IasModules = [
    'Tools',
    'Utils',
    'Cdb',
    'BasicTypes',
    'CdbChecker',
    'KafkaUtils',
    'CommandsAndReplies']

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
        from IasWafBuildTools.Utils import set_env
        set_env(bld.env, bld.path, bld.bldnode)
        from IasWafBuildTools.IasWafBuilder import install
        install(bld)


    else:
        bld.recurse(IasModules)

def configure(conf):
    Logs.info ("IAS: configure in %s",conf.path.abspath())
    conf.load('IasWafBuildTools', tooldir='WafBuildTools/src/main/python/')
    from IasWafBuildTools.IntegrityCheck import checkIntegrity
    checkIntegrity(conf)

    # Build java source with the waf java tool
    conf.load("java")

    conf.recurse(IasModules)

