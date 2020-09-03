top = '.'
out = 'build'

import os

'''
    Since release 12, the IAS is built using the Waf build tool.

    The description of the build procedure is in WafBuildTools/README.md
'''

def build(bld):
    print("Building IAS in",bld.path.abspath())
    bld.recurse('Tools')

def options(conf):
    pass

def configure(conf):
    print ("IAS configure in ",conf.path.abspath())
    conf.load('IasWafBuildTools', tooldir='WafBuildTools/src/main/python/')
    from IasWafBuildTools.IntegrityCheck import checkIntegrity
    checkIntegrity(conf)

    # Build java source with the waf java tool
    conf.load("java")

    conf.recurse('Tools')


    conf.load('IasWafBuildTools', tooldir='WafBuildTools/src/main/python/')
    from IasWafBuildTools.BuildTest import test
    test()