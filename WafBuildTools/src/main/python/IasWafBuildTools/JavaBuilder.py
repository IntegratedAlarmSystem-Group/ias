'''
    Build java sources.

    There is a waf tool to build java source but I prefer to have the process under control
    while mixing java and scala.
'''

# JAVAC options
JAVAC_OPTS = "-Xpkginfo:always"

import glob
from waflib.Task import Task
from IasWafBuildTools.Utils import buildDstFileNode
from IasWafBuildTools.JavaScalaCommBuilder import JavaScalaCommBuilder

def buildJava(env):
    '''
    Helper function to create the Waf task to build java sources

    :param env: The Waf environment
    :return: The Waf rask to build scala
    '''
    assert env
    javaBuilder = JavaBuilder(env)
    javaBuilder.color = 'GREEN'
    return javaBuilder

class JavaBuilder(Task):
    '''
    Build scala sources in .class files to be packed into jar files.

    Reads the scala sources in src/scala/**.
    .class files will be saved in build/scala
    '''

    def __init__(self, environ):
        super(JavaBuilder, self).__init__(env=environ)
        # A dictionary to associate each scala source file to the destination (.class)
        self.filesToBuild = {}
        self.getJavaSources()

    def getJavaSources(self):
        '''
        Scan the scala folder to add to the nputs all the *.scala sources
        :return:
        '''
        # All java sources
        javaSources = self.env.JAVASRCFOLDER.ant_glob("**/*.java")

        for JavaSrc in javaSources:
            # The dest file must take into account subfolders
            destDir = JavaSrc.abspath().replace(self.env.JAVASRCFOLDER.abspath(), "", 1) # Still contains the name of the file
            pos = destDir.rfind('/')
            fName=destDir[pos+1:]
            destDir = destDir[1:pos]
            destNode = self.env.JVMDSTFOLDER.find_or_declare(destDir)
            self.set_inputs(JavaSrc)
            dst = buildDstFileNode(JavaSrc, destNode, dstFileName=fName.replace(".java", ".class"))
            self.set_outputs(dst)
            self.filesToBuild[JavaSrc] = dst

    def addScalaToClasspath(self):
        '''
        Add scala jars to the classpath

        :return: the string to add to the classpath with the jars of scala
        '''
        scala_jars = glob.glob(self.env.SCALA_HOME+"/lib/*.jar", recursive=True)
        ret = ""
        first = True
        for jar in scala_jars:
            if first:
                ret = ret + jar
                first = False
            else:
                ret = ret + ':' + jar
        return ret

    def run(self):
        sourceNodes = self.filesToBuild.keys()
        sourceFiles = map(lambda x: x.abspath(), sourceNodes)
        sourceFiles = " ".join(sourceFiles)

        # Java classpath needs also the classes built by scala in the same module and contained in JVMDSTFOLDER
        classPath = "%s:%s:%s" % (
            JavaScalaCommBuilder.buildClasspath(self.env.BLDNODE.abspath(), self.env.PREFIX),
            self.env.JVMDSTFOLDER.abspath(),
            self.addScalaToClasspath())

        cmd = self.env.JAVAC[0]+" "+JAVAC_OPTS +" -d "+self.env.JVMDSTFOLDER.abspath()+" "+classPath+" "+sourceFiles
        self.exec_command(cmd)
