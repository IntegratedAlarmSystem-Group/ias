# Install the artifacts to the destination folder
#
# Object of the Installer class do not use methods like Build.install_files
# because such methods have no effectduring the build (they work only during waf install)

import os.path, shutil
from pathlib import Path
from glob import iglob
from waflib import Logs
from IasWafBuildTools.FileTasks import CopyTask

class InstallerBase(object):
    '''
    Base class for the installer with linux command and that with
    Waf Task
    '''

    def __init__(self, srcFolder, destFolder):
        '''
        Constructor

        :param srcFolder: Source node folder
        :param destFolder: Destination folder
        :return:
        '''
        assert srcFolder
        assert destFolder
        self.srcFolder = srcFolder
        self.destFolder = destFolder

        Logs.info("Installer: will installs from %s ---to---> %s " % (self.srcFolder, self.destFolder))

        self.foldersToInstall = [ 'bin', 'lib', 'extTools', 'config']

class Installer(InstallerBase):
    '''
    The installer copy files generated by the build into the folder pointed by PREFIX
    using python commands i.e. it does not instantiate Waf tasks
    '''

    def __init__(self, srcFolder, destFolder):
        '''
        Build the Installer.

        :param srcNode: Source node folder
        :param destFolder: Destination folder
        '''
        super(Installer, self).__init__(srcFolder, destFolder)

    def install(self):
        '''
        Build the dictionary of files to install

        :param srcDirs The list of folders containing files to be installed
        :return:
        '''
        Logs.info("Installer: Installing files from %s to %s",  self.srcFolder, self.destFolder)
        for dirToInstall in self.foldersToInstall:
            dest = "%s/%s" % (self.destFolder,dirToInstall)
            Path(dest).mkdir(parents=True, exist_ok=True)

            Logs.info("Installer: processing %s --> %s " % (dirToInstall,dest))

            startDir = "%s/%s" % (self.srcFolder, dirToInstall)
            # if startDir is None:
            #     Logs.debug("Installer: folder %s empty: nothing to install", startDir)
            #     continue

            if not os.path.exists(startDir):
                Logs.info("Installer: No folder %s found", startDir)
                continue

            filesToCopy = iglob(startDir + '**/*', recursive=True)

            for fileToCopy in filesToCopy:
                base = fileToCopy.split(self.srcFolder)
                destName = self.destFolder+base[1]

                # Create folders in the destination if it does not already exist
                destPath = Path(destName).parent
                destPath.mkdir(parents=True, exist_ok=True)
                if not os.path.isdir(fileToCopy): # Sskip creation of directories
                    shutil.copy2(fileToCopy,destName)
                    Logs.info("Installer: %s --> %s",fileToCopy ,destName)

        Logs.info("Installer: Installation from %s done", self.srcFolder)

class InstallerTask(InstallerBase):
    '''
    In        stall files from the source folder to the destination folder
    buy delegating to IasWafBuildTools.FileUtils.CopyTask.
    '''

    def __init__(self, srcFolder, destFolder, ctx):
        '''
        Build the Installer.

        :param srcNode: Source node folder
        :param destFolder: Destination folder
        :param ctx: Waf build context
        '''
        super(InstallerTask, self).__init__(srcFolder, destFolder)
        assert ctx
        self.ctx = ctx

    def install(self):
        Logs.info("Installer: Installing files from %s to %s",  self.srcFolder, self.destFolder)
        srcNodeBase = self.ctx.path.make_node(self.srcFolder)
        dstNodeBase = self.ctx.path.make_node(self.destFolder)
        for dirToInstall in self.foldersToInstall:
            src = "%s/%s" % (self.srcFolder, dirToInstall)

            if not not os.path.exists(src):
                Logs.info("Installer: source folder %s is empty: skipped", src)
                continue

            srcNode = srcNodeBase.search_node(dirToInstall)
            dstNode = dstNodeBase.make_node(dirToInstall)

            Logs.debug("Installer: processing %s",srcNode.abspath())

            copyTask = CopyTask(
                self.ctx.env,
                src_folder_node= srcNode,
                dst_folder_node = dstNode,
                recurse= True
            )

            self.ctx.add_to_group(copyTask)