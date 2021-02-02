'''
    Build scala sources
'''
import os.path
from waflib.Task import Task
from IasWafBuildTools.Utils import buildDstFileNode
from IasWafBuildTools.JavaScalaCommBuilder import JavaScalaCommBuilder

def buildScala(ctx):
    '''
    Helper function to create the Waf task to build scala sources

    :param ctx: The Waf build context
    :return: The Waf rask to build scala
    '''
    scalaBuilder = ScalaBuilder(ctx.env)
    scalaBuilder.color='CYAN'
    return scalaBuilder

class ScalaBuilder(Task):
    '''
    Build scala sources in .class files to be packed into jar files.

    Reads the scala sources in src/scala/**.
    .class files will be saved in build/scala
    '''

    def __init__(self,environ):
        super(ScalaBuilder, self).__init__(env=environ)
        # A dictionary to associate each scala source file to the destination (.class)
        self.filesToBuild = {}
        self.getScalaSources()
        print(">>>> filesToBuild",self.filesToBuild)

    def getScalaSources(self):
        '''
        Scan the scala folder to add to the nputs all the *.scala sources
        :return:
        '''
        # All python sources
        scalaSources = self.env.SCALASRCFOLDER.ant_glob("**/*.scala")
        print('>>> scalaSources', scalaSources)

        for scalaSrc in scalaSources:
            # The dest file must take into account subfolders
            destDir = scalaSrc.abspath().replace(self.env.SCALASRCFOLDER.abspath(),"",1) # Still contains the name of the file
            pos = destDir.rfind('/')
            fName=destDir[pos+1:]
            destDir = destDir[1:pos]
            destNode = self.env.JVMDSTFOLDER.find_or_declare(destDir)
            self.set_inputs(scalaSrc)
            dst = buildDstFileNode(scalaSrc, destNode, dstFileName=fName.replace(".scala",".class"))
            self.set_outputs(dst)
            self.filesToBuild[scalaSrc]=dst

    def run(self):
        print("Running ScalaBuilder with ENV=",self.env)

        sourceNodes = self.filesToBuild.keys()
        sourceFiles =  map(lambda x: x.abspath(), sourceNodes)
        sourceFiles = " ".join(sourceFiles)
        print("Building", sourceFiles)

        classPath = JavaScalaCommBuilder.buildClasspath(self.env.DSTNODE.abspath(), self.env.PREFIX)

        cmd = self.env.SCALAC[0]+" -d "+self.env.JVMDSTFOLDER.abspath()+" "+classPath+" "+sourceFiles
        print(">>> Executing SCALAC: ", cmd)
        self.exec_command(cmd)
