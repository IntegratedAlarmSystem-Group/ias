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


class getExternalJars(Task):
    """
    Get external JARS from extTools folder

    - input: the Node of the ZIP file must be passed as the first item of the inputs list
    - output: the jars to be taken from the zip file must be passed as Node in the outputs list
      (note that the output nodes also contains the path where the jar must be put)

    :return:
    """

    def run(self):
        pass