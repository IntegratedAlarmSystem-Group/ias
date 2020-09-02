top = '.'
out = 'build'

import os
'''
    Check if the IasRoot exists.
    If does not exists, build the folder
    
    @param folderPath the path of IAS_ROOT
    @return True if the folder exists; False if it does not exists and cannot be created  
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