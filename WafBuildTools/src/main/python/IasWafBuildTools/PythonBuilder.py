'''
Python classes to support building of python scripts and modules
'''

from waflib.Task import Task
from IasWafBuildTools.Utils import buildDstFileNode


class BuildPythonModules(Task):
    '''
    Build python modules.

    Python modules are folder of python scripts that will be carbon copied
    from the source folder src/main/python
    to the destination folder build/lib/python<version>/site-packages

    Source folders might be nested.
    '''

    def __init__(self,environ):
        super(BuildPythonModules, self).__init__(env=environ)
        self.getPyModules()

    def getPyModules(self):
        '''
        Scan the python source folder and preprae the list of inputs and outputs to be used by the build.
        :return:
        '''
        # python sources in the main folder (to be discarded)
        pySources = self.env.PYSRCFOLDER.ant_glob("*.py")
        # All python sources
        pyModSources = self.env.PYSRCFOLDER.ant_glob("**/*.py")
        print('>>> pyModSources', pyModSources)

        # A dictionary to associate each input file to the destination
        self.filesToCopy = {}

        for pySrc in pyModSources:
            if pySrc not in pySources:
                # The dest file must take into account subfolders
                destDir = pySrc.abspath().replace(self.env.PYSRCFOLDER.abspath(),"",1) # Still contains the name of the file
                pos = destDir.rfind('/')
                destDir = destDir[1:pos]
                destNode = self.env.PYMODDSTFOLDER.find_or_declare(destDir)
                self.set_inputs(pySrc)
                dst = buildDstFileNode(pySrc, destNode)
                self.set_outputs(dst)
                self.filesToCopy[pySrc]=dst

    def run(self):
        print("ENV=",self.env)

        for s,d in self.filesToCopy.items():
            print('PY SRC',s.abspath(),'=>',d.abspath())
            self.exec_command("cp "+s.abspath()+" "+d.abspath())
