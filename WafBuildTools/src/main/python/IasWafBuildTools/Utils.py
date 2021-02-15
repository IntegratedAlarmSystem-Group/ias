'''
A collection of utilities for Waf build system of the IAS
'''

def getPythonVersion():
    '''
    Get the version of the installed python executable

    :return: The version of python as list of strings ['major', 'minor', 'patch']
    '''
    import subprocess
    out=subprocess.Popen(['python', '--version'],
                     stdout=subprocess.PIPE,
                     stderr=subprocess.STDOUT)
    stdout,stderr = out.communicate()

    return stdout.decode("utf-8").strip().split(" ")[1].split(".")

def set_env(env, sourceNode, buildNode):
    '''
    Adds to the passed environment, a set variables useful for the
    building of the IAS.

    In particular all the folders used by the build
    :param env: the environment
    :param sourceNode: the waf node of the source folder (root of subproject like oas/Tools)
    :param buildNode: the waf node of the folder of the build (like ias/Supervisor/build)
    :return:
    '''
    if not env:
        raise ValueError("The environment can't be None")
    if not sourceNode:
        raise ValueError("Invalid None source node")
    if not buildNode:
        raise ValueError("Invalid None build node")

    env.BLDNODE = buildNode
    env.SRCNODE = sourceNode
    parts = env.SRCNODE.abspath().split('/')
    env.MODULENAME = parts[len(parts)-1]
    env.DSTNODE = buildNode.make_node(env.MODULENAME)
    # print("env.SRCNODE", env.SRCNODE)
    # print("env.BLDNODE", env.BLDNODE)
    # print("env.DSTNODE", env.DSTNODE)
    # print("env.MODULENAME", env.MODULENAME)

    env.SRCEXTTOOLSFOLDER = sourceNode.make_node('extTools')
    env.BLDEXTTOOLSFOLDER = buildNode.make_node('extTools')
    # print("env.SRCEXTTOOLSFOLDER", env.SRCEXTTOOLSFOLDER.abspath())
    # print("env.BLDEXTTOOLSFOLDER", env.BLDEXTTOOLSFOLDER.abspath())

    env.SRCMAINFOLDER = sourceNode.make_node('src/main')
    # print("env.SRCMAINFOLDER", env.SRCMAINFOLDER)

    env.BLDBINFOLDER = buildNode.make_node('bin')
    env.BLDLIBFOLDER = buildNode.make_node('lib')
    # print("env.BLDBINFOLDER", env.BLDBINFOLDER.abspath())
    # print("env.BLDLIBFOLDER", env.BLDLIBFOLDER.abspath())

    env.PYSRCFOLDER = sourceNode.make_node('src/main/python')

    py_version = getPythonVersion()
    env.PYMODDSTFOLDER = env.BLDLIBFOLDER.make_node('python{}.{}/site-packages'.format(py_version[0],py_version[1]))
    # print("env.PYSRCFOLDER", env.PYSRCFOLDER.abspath())
    # print("env.PYMODDSTFOLDER", env.PYMODDSTFOLDER.abspath())

    env.CONFIGSRCFOLDER = sourceNode.make_node('config')
    env.CONFIGDSTFOLDER =  buildNode.make_node('config')
    # print("env.CONFIGSRCFOLDER", env.CONFIGSRCFOLDER.abspath())
    # print("env.CONFIGDSTFOLDER", env.CONFIGDSTFOLDER.abspath())

    env.JAVASRCFOLDER = sourceNode.make_node('src/main/java')
    # print("env.JAVASRCFOLDER", env.JAVASRCFOLDER.abspath())

    env.SCALASRCFOLDER = sourceNode.make_node('src/main/scala')
    # print("env.SCALASRCFOLDER", env.SCALASRCFOLDER.abspath())

    env.JVMDSTFOLDER = env.DSTNODE.make_node('classes') # For .class files
    # print("env.JVMDSTFOLDER", env.JVMDSTFOLDER.abspath())

def buildDstFileNode(inputNode, dstFolderNode, dstFileName=None, removeExtension=False):
    '''
    Build the destination node of the file in input.

    The destination goes in the dstFolderNode with the dstFileName name
    :param inputNode: the waf node of the file in input
    :param dstFolderNode: the waf node of the destination folder
    :param dstFileName: the name (string) of the destination file
                        if None, the name of the source is used for the destination
    :param removeExtension: if True the destination file name has no extension (i.e. build.py will become build)
    :return: the waf node of the destination file
    '''
    if not inputNode:
        raise ValueError('Invalid input node')
    if not dstFolderNode:
        raise ValueError('Invalid output folder node')

    if not dstFileName:
        temp = inputNode.abspath().split('/')
        dstFileName = temp[len(temp)-1]

    if removeExtension:
        pos = dstFileName.rfind('.')
        if (pos>-1):
            dstFileName= dstFileName[:pos]

    return dstFolderNode.find_or_declare(dstFileName)
