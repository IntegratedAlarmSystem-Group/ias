'''
Build executable scripts in build/bin
'''

from waflib.Task import Task
from IasWafBuildTools.Utils import buildDstFileNode

class BuildExecScripts(Task):
    '''
    Build the executable scripts
    1. copy the files in build/bin
    2. remove the .py extension
    3 ensure they are executables
    '''

    def __init__(self, environ, extension, srcFolderNode, dstFolderNode, removeExtension=True, addPermission=True):
        """
        Constructor

        :param environ: The environment to pass to the superclass
        :param extension: the extension of the sources (.py, .sh, ...)
        :param srcFolderNode: the node of the folder that contains the executables
        :param dstFolderNode: the destination folder of the executable (usually build/bin)
        :param removeExtension: if True the remove the extension in the destination
        :param addPermission: if True add linux permission to execute the script
        """
        super(BuildExecScripts, self).__init__(env=environ)

        self.extension=extension
        self.srcFolderNode=srcFolderNode
        self.dstFolderNode=dstFolderNode
        self.removeExtension=removeExtension
        self.addPermission=addPermission

        # A dictionary to associate each input file to the destination
        self.filesToCopy = {}

        self.getSources()

    def getSources(self):
        '''
        Check if the src folder contains scripts to build and, if it is the case,
        add them to the list of the sources to build and the outputs to create
        :return:
        '''
        print("Getting {} sources from {}".format(self.extension,self.srcFolderNode.abspath()))

        sources = self.srcFolderNode.ant_glob(self.extension)
        print("Executable scripts to build", sources)
        self.set_inputs(sources)

        for execSrc in  sources:
            dst = buildDstFileNode(execSrc, self.dstFolderNode,None,self.removeExtension)

            self.set_outputs(dst)
            self.filesToCopy[execSrc] = dst

    def run(self):
        for s,d in self.filesToCopy.items():
            print('EXEC SRC', s.abspath(), '=>', d.abspath())
            self.exec_command("cp "+s.abspath()+" "+d.abspath())
            if self.addPermission:
                self.exec_command("chmod u+x "+d.abspath())