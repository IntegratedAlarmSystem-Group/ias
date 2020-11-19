top = '.'
out = 'build'

# IAS modules to build
IasModules = 'Tools Utils'

import os

'''
    Since release 12, the IAS is built using the Waf build tool.

    The description of the build procedure is in WafBuildTools/README.md
'''

def options(ctx):
    pass
    # installDir = os.environ['IAS_ROOT']
    # print("\n>>>>>>>>> Setting PREFIX to",installDir,"/n")
    # ctx.add_option('--prefix', action='store', default=installDir, dest=ctx.PREFIX, help='Silly test')

def build(bld):
    bld.env.PREFIX = os.environ['IAS_ROOT']
    print("Building IAS in",bld.path.abspath())
    print('=====>>> ROOT is_install',bld.is_install)
    print('=====>>> build command',bld.cmd)
    print('=====>>> PREFIX', bld.env.PREFIX)
    bld.recurse(IasModules)

def configure(conf):
    print ("IAS configure in ",conf.path.abspath())
    conf.load('IasWafBuildTools', tooldir='WafBuildTools/src/main/python/')
    from IasWafBuildTools.IntegrityCheck import checkIntegrity
    checkIntegrity(conf)

    # Build java source with the waf java tool
    conf.load("java")

    conf.recurse(IasModules)

