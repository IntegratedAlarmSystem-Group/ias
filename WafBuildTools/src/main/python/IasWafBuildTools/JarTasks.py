"""
A set of Waf tasks to deal with jars

The output dir must be passed in env.OUT_PATH
"""

import os
import errno
from pathlib import Path
from tempfile import mkdtemp
from waflib.Task import Task
from waflib import Logs

def getJarsFromExtTools(env):
    '''
    Helper method that creates a task to get the jars from extTools

    :param env Waf environment
    '''
    assert env
    from IasWafBuildTools.FileTasks import CopyTask
    copyExtJarTask = CopyTask(env, env.SRCEXTTOOLSFOLDER, env.BLDEXTTOOLSFOLDER, file_extension=".jar")
    copyExtJarTask.color = 'CYAN'
    return copyExtJarTask

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

        Logs.info("JarTasks: Archive file: %s", self.inputs[0].abspath())

        Logs.info("JarTasks: Files to get from archive: %s", ', '.join(map(lambda x: x.abspath(), self.outputs)))
        Logs.debug('JarTasks: Command to unpack archive: %s',self.cmdToExtractFiles)

        # The dictionary associates the names of the files to their destination paths
        cleanedFileNames = {} # For example {f.out -> /a/b/c/f.out}
        for j in self.outputs:
            p = Path(j.abspath())
            cleanedFileNames[p.name]=j.abspath()

        # get the output folder from one of the files
        outDir = self.outputs[0].abspath()[:self.outputs[0].abspath().rfind('/')+1]
        Logs.debug("JarTasks: Output folder: %s", outDir)

        tempFolder = mkdtemp(prefix="getFilesFromArchive.", suffix=".temp")
        Logs.debug("JarTasks: Using temporary folder: %s", tempFolder)

        Logs.debug("JarTasks: Unzipping", self.inputs[0].abspath())
        self.exec_command(self.cmdToExtractFiles.format(tempFolder))

        Logs.debug("JarTasks: Extracting files")
        for fileName in cleanedFileNames.keys():
            files = sorted(Path(tempFolder).rglob(fileName))

            if not files:
                raise FileNotFoundError(errno.ENOENT, os.strerror(errno.ENOENT), fileName)
            elif len(files)>1:
                raise Exception("The archive contains "+len(files)+" occurrences of "+fileName+": "+files)
            else:
                # File found
                Logs.debug('JarTasks: File %s will be copied in %s', files[0].name, cleanedFileNames[fileName])
                self.exec_command("cp "+os.fspath(files[0].resolve())+" "+cleanedFileNames[fileName])

        # Remove the temp folder
        Logs.debug("JarTasks: Removing temporary folder %s",tempFolder)
        self.exec_command("rm -rf "+tempFolder)

def jarsFromZip(zipFileName, jars, env):
    '''
    Helper function to build a GetJarsFromZip task

    :param: zipFileName the ZIP archive to get jars from
    :param: jars The list of jar files to get
    :param: env The waf environment (usually bld.env)
    :return: The Waf Task to get the jars from the ZIP archive
    '''
    jarsFromZipTask = GetJarsFromZip(env=env)
    jarsFromZipTask.color='YELLOW'
    jarsFromZipTask.set_inputs(env.SRCEXTTOOLSFOLDER.find_node(zipFileName))
    for jar in jars:
        jarsFromZipTask.set_outputs(env.BLDEXTTOOLSFOLDER.find_or_declare(jar))
    return jarsFromZipTask

class GetJarsFromZip(getFilesFromArchive):
    """
        Get the jars from the ZIP file in input.

        - input: the Node of the ZIP file must be passed as the first and only item of the inputs list
        - output: the jars to be taken from the zip file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """
    def run(self):
        # Delegates to GetFilesFromArchive.run()
        self.set_command("unzip -qq -d {} "+self.inputs[0].abspath())
        super(GetJarsFromZip, self).run()

def jarsFromTar(tarFileName, jars, env):
    '''
    Helper function to instantiate a getJarsFromTar Task to get
    a list of jars from a tar (can be compressed)

    :param: tarFileName The name of the tar archive with the jars
    :param: jars The list of jars to get from the archive
    :param: runAfterTask runAfterTask
    :param: env The Waf environment (usually bld.env)

    :return: the Task to get the jars from the tar file
    '''
    tarFileNode = env.SRCEXTTOOLSFOLDER.find_node(tarFileName)

    from IasWafBuildTools.JarTasks import GetJarsFromTar
    getJarsFromTarTask = GetJarsFromTar(env=env)
    getJarsFromTarTask.set_inputs(tarFileNode)
    for jar in jars:
        getJarsFromTarTask.set_outputs(env.BLDEXTTOOLSFOLDER.find_or_declare(jar))
    getJarsFromTarTask.color='PINK'
    return getJarsFromTarTask

class GetJarsFromTar(getFilesFromArchive):
    """
        Get the jars from the TAR file in input

        - input: the Node of the TAR file must be passed as the first and only item of the inputs list
        - output: the jars to be taken from the tar file must be passed as Node in the outputs list
                  (note that the output nodes also contains the path where the jar must be copied)
    """
    def run(self):
        # Delegates to GetFilesFromArchive.run()
        self.set_command("tar xf "+self.inputs[0].abspath()+" --directory {}")
        super(GetJarsFromTar, self).run()

def buildJar(env, jarName):
    '''
    Helper function to create the Waf task to build the jar of scala and java sources

    :param env: The Waf build context
    :param jarName: the name of the jar to build
    :return: The Waf task
    '''
    assert env
    assert jarName
    jarBuilder = CreateJar(env, env.BLDLIBFOLDER, jarName)
    jarBuilder.color = 'BLUE'
    return jarBuilder

class CreateJar(Task):
    '''
    Build a jar file with
    - all the .class files found in the classes folder
    - the files in the resources folder
    '''

    def __init__(self, environ, destFolderNode, jarName, mainClass=None):
        '''
        Constructor

        :param destFolderNode: The folder node to write the jar file into
        :param jarName: The name of the jar to create
        :param mainClass: The optional main class of the jar (to be added to the Manifest)
        '''
        super(CreateJar, self).__init__(env=environ)
        self.destFolderNode = destFolderNode
        self.jarName = jarName
        self.mainClass = mainClass
        self.resources = []

        self.getJarSources()
        self.jarNode = self.destFolderNode.find_or_declare(jarName)
        self.set_outputs(self.jarNode)


    def getJarSources(self):
        '''
        Build the inputs and the outputs of this task

        The inputs are all the .class files in the self.classesSrcNode folder
        The output is the jar file to be written in the self.destFolderNode
        '''
        classSources = self.env.JVMDSTFOLDER.ant_glob("**/*.class")
        for classSrc in classSources:
            self.set_inputs(classSrc)
        if os.path.exists(self.env.RESOURCESFOLDER.abspath()):
            self.resources = self.env.RESOURCESFOLDER.ant_glob("**/*")
            for res in self.resources:
                self.set_inputs(res)

    def run(self):
        '''
        The task to build the jar
        :return:
        '''
        cmd = "%s cf %s" % (self.env.JAR[0], self.jarNode.abspath())
        if self.mainClass is not None:
            cmd = cmd + ' --main-class='+self.mainClass
        cmd = cmd + ' -C '+self.env.JVMDSTFOLDER.abspath()+' .'
        self.exec_command(cmd)

        if len(self.resources) > 0:
            cmd = "%s uf %s" % (self.env.JAR[0], self.jarNode.abspath())
            cmd = cmd + ' -C '+self.env.RESOURCESFOLDER.abspath() + ' .'
            self.exec_command(cmd)



