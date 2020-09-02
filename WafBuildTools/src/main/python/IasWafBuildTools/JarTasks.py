"""
A set of Waf tasks to deal with jars

The output dir must be passed in env.OUT_PATH
"""

import os
import errno
from pathlib import Path
from tempfile import mkdtemp
from waflib.Task import Task


class getJarsFromZip(Task):
    """
        Get the jars from the ZIP file in input

        - input: the Node of the ZIP file must be passed as the first and only item of the inputs list
        - output: the jars to be taken from the zip file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """
    def run(self):
        print("ZIP file to scan:", self.inputs)
        print("JARS to get from zip file:", self.outputs)

        # The dictionary associates the names of the files to their destination paths
        cleanedFileNames = {} # For example {f.out -> /a/b/c/f.out}
        for j in self.outputs:
            p = Path(j.abspath())
            cleanedFileNames[p.name]=j.abspath()

        # get the output folder from one of the files
        outDir = self.outputs[0].abspath()[:self.outputs[0].abspath().rfind('/')+1]
        print("Output folder:", outDir)

        tempFolder = mkdtemp(prefix="getJarsFromZip.", suffix=".temp")
        print("Using temporary folder", tempFolder)

        print("Unzipping",self.inputs[0].abspath())
        self.exec_command("unzip -qq -d "+tempFolder+" "+self.inputs[0].abspath())

        print("Extracting jars")
        for fileName in cleanedFileNames.keys():
            files = sorted(Path(tempFolder).rglob(fileName))

            print("+++ ",fileName,"===>>>",files)
            if not files:
                raise FileNotFoundError( errno.ENOENT, os.strerror(errno.ENOENT), fileName)
            elif len(files)>1:
                raise Exception("The archive contains "+len(files)+" occurrences of "+fileName+": "+files)
            else:
                # File found
                print('File', files[0].name, " will be copied in", cleanedFileNames[fileName])
                self.exec_command("cp "+os.fspath(files[0].resolve())+" "+cleanedFileNames[fileName])

        # Remove the temp folder
        print("Removing temporary folder",tempFolder)
        self.exec_command("rm -rf "+tempFolder)


class getJarsFromTar(Task):
    """
        Get the jars from the TAR file in input

        - input: the Node of the TAR file must be passed as the first and only item of the inputs list
        - output: the jars to be taken from the tar file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """
    def run(self):
        print("ZIP file to scan:", self.inputs)
        print("JARS to get from zip file:", self.outputs)

        # The dictionary associates the names of the jar to their destination paths
        cleanedJarNames = {}
        for j in self.outputs:
            p = Path(j.abspath())
            cleanedJarNames[p.name]=j.abspath()

        # get the output folder from one of the jars
        outDir = self.outputs[0].abspath()[:self.outputs[0].abspath().rfind('/')+1]
        print("Output folder:",outDir)

        tempFolder = mkdtemp(prefix="getJarsFromTar.", suffix=".temp")
        print("Using temporary folder", tempFolder)

        print("Un-tarring",self.inputs[0].abspath())
        self.exec_command("tar xf "+self.inputs[0].abspath()+" --directory "+tempFolder)

        print("Extracting jars")
        for path in Path(tempFolder).rglob('*.jar'):
            if path.name in cleanedJarNames:
                print('Found', path.name, "in", self.inputs[0].abspath())
                self.exec_command("cp "+os.fspath(path.resolve())+" "+cleanedJarNames[path.name])

        # Remove the temp folder
        print("Removing temporary folder",tempFolder)
        self.exec_command("rm -rf "+tempFolder)


class copyFiles(Task):
    """
    Copy input files in output files

    This task copies the input nodes in the output nodes.
    Each input is copied in the output i.e. self.inputs[0] is copied in self.outputs[0] (this allows for renaming):
    it means that the order of the inputs and outputs list is important.

    The number of inputs must be equal to the number of outputs.

    - self.input: the Node of the ZIP file must be passed as the first item of the inputs list
    - self.outputs: the jars to be taken from the zip file must be passed as Node in the outputs list
      (note that the output nodes also contains the path where the jar must be put)

    :return:
    """

    def run(self):
        print("copyFiles")
        ins = list(map(lambda x: x.abspath(), self.inputs))
        outs= list(map(lambda x: x.abspath(), self.outputs))

        print(ins)
        print(outs)

        if len(ins)!=len(outs):
            print("Number of inputs and outputs differ")
            return

        for i in range(len(ins)):
            print("cp",ins[i],"=>",outs[i])
            self.exec_command("cp "+ins[i]+" "+outs[i])
