"""
A set of Waf tasks to deal with jars

The output dir must be passed in env.OUT_PATH
"""

import os
import errno
from pathlib import Path
from tempfile import mkdtemp
from waflib.Task import Task


class getFilesFromArchive(Task):
    """
        Get a set of files from an archive

        The file to be taken are passed as nodes in the outputs list and are saved in the extTools folder.

        This class is the base class of getJarsFromZip and getJarsFromTar that only differ for the command used
        to get the files out of a ZIP or TAR archive.
        The command to extract must be passed by calling getFilesFromArchive.setCommand.
        This allows, if needed, to extract from different types of archives passing the proper command.

        - input: the Node of the archive must be passed as the first item of the inputs list
        - output: the jars to be taken from the zip file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """

    cmdToExtractFiles = None

    def set_command(self, cmd):
        """
        Set the command to unpack the archive

        The folder to unpack is set by running this task to a temporary folder: the command
        must contain "{}" as place holder to add the path of the destination folder usgin the str.format()
        method. If destination folder is not needed, then cmd must not contain {} as str.format would not
        change the cmd string.

        :param cmd: The command
        :return:
        """
        self.cmdToExtractFiles = cmd

    def run(self):
        if not self.cmdToExtractFiles:
            raise Exception('Command to extract not set: call setCommand() before running this task')

        print("Archive file:", self.inputs)
        print("Files to get from archive:", self.outputs)
        print('Command to unpack archive',self.cmdToExtractFiles)

        # The dictionary associates the names of the files to their destination paths
        cleanedFileNames = {} # For example {f.out -> /a/b/c/f.out}
        for j in self.outputs:
            p = Path(j.abspath())
            cleanedFileNames[p.name]=j.abspath()

        # get the output folder from one of the files
        outDir = self.outputs[0].abspath()[:self.outputs[0].abspath().rfind('/')+1]
        print("Output folder:", outDir)

        tempFolder = mkdtemp(prefix="getFilesFromArchive.", suffix=".temp")
        print("Using temporary folder", tempFolder)

        print("Unzipping", self.inputs[0].abspath())
        self.exec_command(self.cmdToExtractFiles.format(tempFolder))

        print("Extracting files")
        for fileName in cleanedFileNames.keys():
            files = sorted(Path(tempFolder).rglob(fileName))

            if not files:
                raise FileNotFoundError(errno.ENOENT, os.strerror(errno.ENOENT), fileName)
            elif len(files)>1:
                raise Exception("The archive contains "+len(files)+" occurrences of "+fileName+": "+files)
            else:
                # File found
                print('File', files[0].name, " will be copied in ==>", cleanedFileNames[fileName])
                self.exec_command("cp "+os.fspath(files[0].resolve())+" "+cleanedFileNames[fileName])

        # Remove the temp folder
        print("Removing temporary folder",tempFolder)
        self.exec_command("rm -rf "+tempFolder)


class getJarsFromZip(getFilesFromArchive):
    """
        Get the jars from the ZIP file in input.

        - input: the Node of the ZIP file must be passed as the first and only item of the inputs list
        - output: the jars to be taken from the zip file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """
    def run(self):
        # Delegates to getFilesFromArchive.run()
        self.set_command("unzip -qq -d {} "+self.inputs[0].abspath())
        super(getJarsFromZip, self).run()


class getJarsFromTar(getFilesFromArchive):
    """
        Get the jars from the TAR file in input

        - input: the Node of the TAR file must be passed as the first and only item of the inputs list
        - output: the jars to be taken from the tar file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """
    def run(self):
        # Delegates to getFilesFromArchive.run()
        self.set_command("tar xf "+self.inputs[0].abspath()+" --directory {}")
        super(getJarsFromTar, self).run()
