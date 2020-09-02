"""
A set of Waf tasks to deal with jars

The output dir must be passed in env.OUT_PATH
"""

import os
from pathlib import Path
from waflib.Task import Task


class getJarsFromZip(Task):
    """
        Get the jars from the ZIP file in input

        - input: the Node of the ZIP file must be passed as the first item of the inputs list
        - output: the jars to be taken from the zip file must be passed as Node in the outputs list
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

        tempFolder = outDir+"temp"

        print("Unzipping",self.inputs[0].abspath())
        self.exec_command("unzip -qq -d "+tempFolder+" "+self.inputs[0].abspath())

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
    Get external JARS from extTools folder

    This task copies the input nodes in the output nodes.
    Each input is copied in the output i.e. self.inputs[0] is copied in self.outputs[0] (this allows for renaming)
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
