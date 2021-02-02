'''
    Build scala sources.

    There is a waf tool to build java source but I prefer to have the process under control
    while mixing java and scala.
'''

from waflib.Task import Task
from IasWafBuildTools.Utils import buildDstFileNode
from IasWafBuildTools.JavaScalaCommBuilder import JavaScalaCommBuilder

def buildJava(ctx):
    '''
    Helper function to create the Waf task to build scala sources

    :param ctx: The Waf build context
    :return: The Waf rask to build scala
    '''
    javaBuilder = JavaBuilder(ctx.env)
    javaBuilder.color='GREEN'
    return javaBuilder

class JavaBuilder(Task):
    '''
    Build scala sources in .class files to be packed into jar files.

    Reads the scala sources in src/scala/**.
    .class files will be saved in build/scala
    '''

    def __init__(self,environ):
        super(JavaBuilder, self).__init__(env=environ)
        # A dictionary to associate each scala source file to the destination (.class)
        self.filesToBuild = {}
        self.getJavaSources()
        print(">>>> filesToBuild",self.filesToBuild)

    def getJavaSources(self):
        '''
        Scan the scala folder to add to the nputs all the *.scala sources
        :return:
        '''
        # All java sources
        javaSources = self.env.JAVASRCFOLDER.ant_glob("**/*.java")
        print('>>> javaSources', javaSources)

        for JavaSrc in javaSources:
            # The dest file must take into account subfolders
            destDir = JavaSrc.abspath().replace(self.env.JAVASRCFOLDER.abspath(),"",1) # Still contains the name of the file
            pos = destDir.rfind('/')
            fName=destDir[pos+1:]
            destDir = destDir[1:pos]
            destNode = self.env.JVMDSTFOLDER.find_or_declare(destDir)
            self.set_inputs(JavaSrc)
            dst = buildDstFileNode(JavaSrc, destNode, dstFileName=fName.replace(".java",".class"))
            self.set_outputs(dst)
            self.filesToBuild[JavaSrc] = dst

    def run(self):
        print("Running JavaBuilder with ENV=", self.env)

        sourceNodes = self.filesToBuild.keys()
        sourceFiles = map(lambda x: x.abspath(), sourceNodes)
        sourceFiles = " ".join(sourceFiles)
        print("Building", sourceFiles)

        classPath = JavaScalaCommBuilder.buildClasspath(self.env.DSTNODE.abspath(), self.env.PREFIX)

        cmd = self.env.JAVAC[0]+" -d "+self.env.JVMDSTFOLDER.abspath()+" "+classPath+" "+sourceFiles
        print ("|>>> Executing JAVAC: ", cmd)
        self.exec_command(cmd)
