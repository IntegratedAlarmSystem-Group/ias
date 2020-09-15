'''
A set of tasks for files
'''

from waflib.Task import Task

class CopyTask(Task):
    '''
    Copy files from one source folder to a folder with the same name in the destination folder or
    in the passed (not None) destination folder

    For example it copies all the files from config into build/config

    Optionally, it can remove the extension from the destination file and
    make it executable. Note thea this class removes the extension i.e. whatever appears after the
    last dot in the file name it.e. it removes the extension from cmd.py AND cmd2.py

    :param Task:
    :return:
    '''

    def __init__(
            self,
            environ,
            src_folder_node,
            dst_folder_node=None,
            file_extension=None,
            recurse=False,
            remove_etension=False,
            make_executable=False):
        '''
        Build
        :param self:
        :param environ: the environment
        :param srcFolder: The Node of the source folder
        :para dst_folder_node: The destination folder to copy the files into
                               If Note the files are copied in the same folder into build
                               otherwise they are copied in the passed folder
        :param file_extension: if not None copy only the files with the given pattern
                               (for example passing *.py copy only the files with .py extension)
        :param recurse: if True the files are copied recursively otherwise on
                        the files in the main folder are copied
        :param remove_etension: if True removes the extension from the destination file name
                                i.e. myCmd.sh becomes myCmd
                                Note that the extension is whatever comes after the last dot of the file name
        :param make_executable: if True the destination file is made executable
        :return:
        '''
        super(CopyTask, self).__init__(env=environ)
        self.srcFolderNode=src_folder_node
        self.dstFolderNode = dst_folder_node
        if file_extension is None:
            self.extension = ""
        else:
            self.extension = file_extension
        self.recurse=recurse
        self.removeExt = remove_etension
        self.makeExec = make_executable
        self.filesToCopy = self.__buildNodes()


    def __removeExtension(self,fileName):
        """
        Remove the extension for the passed name i.e. if the input is fff.xyz, it return fff

        :param fileName: the name of the file
        :return: the file name without extension
        """
        idx = fileName.find(".")
        if idx == -1:
            # The file has no extension
            return fileName
        else:
            return fileName[:idx]

    def __buildNodes(self):
        '''
        Scans the source folder to build the Nodes in the destination folder

        :return:
        '''
        if not self.recurse:
            sources = self.srcFolderNode.ant_glob("*"+self.extension)
        else:
            sources = self.srcFolderNode.ant_glob('**/*'+self.extension)
        self.set_inputs(sources)

        filesToCopy = {}
        for s in sources:
            if self.dstFolderNode is None:
                filesToCopy[s.get_src().abspath()] = s.get_bld().abspath()

                if self.removeExt:
                    filesToCopy[s.get_src().abspath()] = self.__removeExtension(s.get_bld().abspath())
                    self.set_outputs(self.env.DSTNODE.find_or_declare(self.__removeExtension(s.get_bld().abspath()).split(self.env.DSTNODE.abspath()+"/")[1]))
                else:
                    filesToCopy[s.get_src().abspath()] =s.get_bld().abspath()
                    self.set_outputs(self.env.DSTNODE.find_or_declare(s.get_bld().abspath().split(self.env.DSTNODE.abspath()+"/")[1]))
            else:
                if self.removeExt:
                    absFileName = self.__removeExtension(s.get_src().abspath().split(self.srcFolderNode.abspath()+"/")[1])
                else:
                    absFileName = s.get_src().abspath().split(self.srcFolderNode.abspath()+"/")[1]
                dstFileName = self.dstFolderNode.abspath()+"/"+absFileName
                filesToCopy[s.get_src().abspath()] = dstFileName
                self.set_outputs(self.dstFolderNode.find_or_declare(absFileName))

        return filesToCopy

    def run(self):
        print("Files to copy", self.filesToCopy)

        for s,d in self.filesToCopy.items():
            print("FileTask, copying", s, "==>", d)
            self.exec_command("cp "+s+" "+d)
            if self.makeExec:
                self.exec_command("chmod u+x "+d)
