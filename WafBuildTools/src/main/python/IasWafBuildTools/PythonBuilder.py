'''
Python classes to support building of python scripts and modules
'''

from waflib.Task import Task
from IasWafBuildTools.Utils import buildDstFileNode

class BuildPythonScripts(Task):
    '''
    Build the python scripts that are in src/main/python:
    1. copy the files in build/bin
    2. remove the .py extension
    3 ensure they are executables
    '''

    def getPySources(self):
        '''
        Check if the src folder contains python scripts to build and, if it is the case,
        add them to the list of the sources to build and the outputs to create
        :return:
        '''
        print("Getting python sources")

        # A dictionary to associate each input file to the destination
        self.filesToCopy = {}

        pySources = self.env.PYSRCFOLDER.ant_glob("*.py")
        print("Python scripts to build",pySources)
        self.set_inputs(pySources)

        for pySrc in  pySources:
            dst = buildDstFileNode(pySrc, self.env.BLDBINFOLDER,None,True)

            self.set_outputs(dst)
            self.filesToCopy[pySrc]=dst



    def run(self):
        print("ENV=",self.env)

        for s,d in self.filesToCopy.items():
            print('PY SRC',s.abspath(),'=>',d.abspath())
            self.exec_command("cp "+s.abspath()+" "+d.abspath())
            self.exec_command("chmod u+x "+d.abspath())


class BuildPythonModules(Task):
    '''
    Build python modules.

    Python modules are folder of python scripts that will be carbon copied
    from the source folder src/main/python
    to the destination folder build/lib/python<version>/site-packages

    Source folders might be nested.
    '''

    def getPyModules(self):
        '''
        Scan the python source folder and preprae the list of inputs and outputs to be used by the build.
        :return:
        '''
        # python sources in the main folder (to be discarded)
        pySources = self.env.PYSRCFOLDER.ant_glob("*.py")
        # All python sources
        pyModSources = self.env.PYSRCFOLDER.ant_glob("**/*.py")
        print('>>> pyModSources',pyModSources)

        # A dictionary to associate each input file to the destination
        self.filesToCopy = {}

        for pySrc in pyModSources:
            if pySrc not in pySources:
                # The dest file must take into account subfolders
                destDir = pySrc.abspath().replace(self.env.PYSRCFOLDER.abspath(),"",1) # Still contains the name of the file
                pos = destDir.rfind('/')
                destDir = destDir[1:pos]
                destNode = self.env.PYMODDSTFOLDER.find_or_declare(destDir)
                print('>>>>',destDir, ">>>>>",destNode.abspath())
                self.set_inputs(pySrc)
                dst = buildDstFileNode(pySrc, destNode)
                self.set_outputs(dst)
                self.filesToCopy[pySrc]=dst
                print('>>>>',pySrc.abspath()," ==> ",dst.abspath())

    def run(self):
        print("ENV=",self.env)

        for s,d in self.filesToCopy.items():
            print('PY SRC',s.abspath(),'=>',d.abspath())
            self.exec_command("cp "+s.abspath()+" "+d.abspath())
