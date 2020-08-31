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
          (note that the output nodes also contains the path where the jar must be put)
    """
    def run(self):
        print("ZIP file to scan:", self.inputs)
        print("JARS to get from zip file:", self.outputs)

        # The dictionary associates the names of the jar to their destination paths
        cleanedJarNames = {}
        for j in self.outputs:
            p = Path(j.abspath())
            cleanedJarNames[p.name]=j.abspath()

        tempFolder = "temp"

        self.exec_command("unzip -qq -d "+tempFolder+" "+self.inputs[0].abspath())
        print("self.env.OUT_PATH", self.env.OUT_PATH)
        #self.exec_command('find '+tempFolder+' -name "*.jar" >'+tempFolder+'/listOfJars.out')

        for path in Path(self.env.OUT_PATH+"/"+tempFolder).rglob('*.jar'):
            if path.name in cleanedJarNames:
                print('Found', path.name, "in", self.inputs[0].abspath())
                self.exec_command("cp "+os.fspath(path.resolve())+" "+cleanedJarNames[path.name])

        # Remove the temp folder
        self.exec_command("rm -rf "+tempFolder)
