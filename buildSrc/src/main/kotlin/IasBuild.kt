package org.eso.ias.build.plugin

import java.io.File as JavaFile
import java.nio.file.attribute.PosixFilePermissions

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.konan.file.File

import org.eso.ias.build.plugin.GuiBuilder

open class IasBuild : Plugin<Project> {

    override fun apply(project: Project) {

        val logger = project.getLogger()
        logger.info("IAS build plugin in {}", project.getLayout().getBuildDirectory())

        // Get the python version from the property in settings.gradle.kts
        val g = project.gradle
        val pythonVersion = (g as ExtensionAware).extra["PythonVersion"]
        logger.info("Using python version {}", pythonVersion)

        // Configurations
        val conf = project.tasks.register<Copy>("CopyConfigFiles") {
            val confFolder = "config"
            val destFolder = "config"

            from(project.layout.projectDirectory.dir(confFolder))
            include("*")

            into(project.layout.buildDirectory.dir(destFolder))
        }

        // Python Scripts
        val pyScripts = project.tasks.register<Copy>("CopyPyScripts") {
            val pyFolder = "src/main/python"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            into(project.layout.buildDirectory.dir(destFolder))
            // Set file permissions directly
            filePermissions {
                user {
                    read = true
                    execute = true
                }
                group {
                    read = true
                    execute = true
                }
                other.execute = true
            }
        }

        // Shell scripts
        val shScripts = project.tasks.register<Copy>("CopyShScripts") {
            val shFolder = "src/main"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(shFolder))
            include("*.sh")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            into(project.layout.buildDirectory.dir(destFolder))
            // Set file permissions directly
            filePermissions {
                user {
                    read = true
                    execute = true
                }
                group {
                    read = true
                    execute = true
                }
                other.execute = true
            }
            
        }


        // Spawn one copy task task for each python modules
        // i.e. each folder in src/main/python
        val pyModules = project.tasks.register<Copy>("CopyPyMods") {

            val srcFolder = "src/main/python"

            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")
            val destFolder = "lib/python${pythonVersion}/site-packages"
            into(project.layout.buildDirectory.dir(destFolder))

            dependsOn(project.tasks.named("assemble"))
        }
        
        val installBin = project.tasks.register<Copy>("InstallBin") {
            var envVar: String? = System.getenv("IAS_ROOT")

            dependsOn(":${project.name}:CopyPyScripts")
            dependsOn(":${project.name}:CopyShScripts")
            // dependsOn(":${project.name}:CopyPyTestScripts")

            from(project.layout.buildDirectory.dir("bin"))
            include("**/*")
            exclude("**/test*")
            val destFolder = "${envVar}/bin"
            into(destFolder)
        }

        val installConfig = project.tasks.register<Copy>("InstallConfig") {
            var envVar: String? = System.getenv("IAS_ROOT")

            dependsOn(":${project.name}:CopyConfigFiles")

            from(project.layout.buildDirectory.dir("config"))
            include("**/*")
            val destFolder = "${envVar}/config"
            into(destFolder)
        }

        val installLib = project.tasks.register<Copy>("InstallLib") {
            var envVar: String? = System.getenv("IAS_ROOT")

            dependsOn(":${project.name}:CopyExtLib")
            dependsOn(":${project.name}:CopyPyMods")
            dependsOn(":${project.name}:CopyPyTestMods")
            dependsOn(":${project.name}:CopyPyGuiModules")

            from(project.layout.buildDirectory.dir("lib"))
            include("**/*")
            exclude("*Test.jar")
            val destFolder = "${envVar}/lib"
            into(destFolder)
        }

        project.tasks.register("install") {
            dependsOn(installConfig)
            dependsOn(installBin)
            dependsOn(installLib)

            var envVar: String? = System.getenv("IAS_ROOT")
            if (envVar==null) {
                throw GradleException("IAS_ROOT undefined")
            }
        }

        // Untar the archive in build/distribution
        val untar = project.tasks.register<Copy>("Untar") {
            dependsOn(":${project.name}:distTar")

            onlyIf {
                // This task must be executed only for java and scala modules
                // for which the distTar task creates the tar
                //
                // So we exclude the task for python only modules
                // (like modules that create GUIs) that do not have the java and scala plugin
                val folder = project.layout.buildDirectory.dir("distributions")
                val tarFileName = folder.get().asFile.path+"/${project.name}.tar"
                
                var file = JavaFile(tarFileName)
                file.exists()
            }

            val folder = project.layout.buildDirectory.dir("distributions")
            val tarFileName = folder.get().asFile.path+"/${project.name}.tar"
            from(project.tarTree(tarFileName))
            into(folder)
        }

        val copyExtLibs = project.tasks.register<Copy>("CopyExtLib") {
            dependsOn(untar)
            from(project.layout.buildDirectory.dir("distributions/${project.name}/lib"))
            into(project.layout.buildDirectory.dir("lib/ExtLibs"))
            exclude("ias*.jar")
        }

        // Copy python test scripts in build/bin
        // val pyTestScripts = project.tasks.register<Copy>("CopyPyTestScripts") {
        //     val pyFolder = "src/test/python"
        //     val destFolder = "bin"

        //     from(project.layout.projectDirectory.dir(pyFolder))
        //     include("*.py")
        //     rename { filename: String ->
        //         filename.substringBefore('.')
        //     }
            
        //     into(project.layout.buildDirectory.dir(destFolder))

        //     // Set file permissions directly
        //     filePermissions {
        //         user {
        //             read = true
        //             execute = true
        //         }
        //         group {
        //             read = true
        //             execute = true
        //         }
        //         other.execute = true
        //     }

        // }

        // Copy python test modules
        // i.e. each folder in src/test/python
        // val pyTestModules = project.tasks.register<Copy>("CopyPyTestMods") {
        //     val srcFolder = "src/test/python"

        //     from(project.layout.projectDirectory.dir(srcFolder))
        //     include("**/*.py")
        //     exclude("*.py")
        //     val destFolder = "lib/python${pythonVersion}/site-packages"
        //     into(project.layout.buildDirectory.dir(destFolder))
        // }

        // Build PySide6 code like .ui and .qrc by delegating to GuiBuilder
        val pyside6GuiBuilder = project.tasks.register("GuiBuilder") {
            dependsOn(pyModules)
            onlyIf {
                var folder = "src/main/gui"
                val f = project.layout.projectDirectory.dir(folder)
                f.getAsFile().exists()
            }
            var folder = "src/main/gui"
            val guiFolder = project.layout.projectDirectory.dir(folder).getAsFile().getPath()
            logger.info("Building GUI in {}", guiFolder)
            val destFolder = "lib/python${pythonVersion}/site-packages"
            val directory = project.layout.projectDirectory.dir(destFolder).getAsFile().getPath()
            val guiBuilder = GuiBuilder(guiFolder, directory)
            guiBuilder.build()
        }

        // Copy the python files generated building PySide6 resources
        // in the build folder
        val copyPyGuiModules = project.tasks.register<Copy>("CopyPyGuiModules") {
            dependsOn(pyside6GuiBuilder)

            val srcFolder = "src/main/gui"

            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")
            val destFolder = "lib/python${pythonVersion}/site-packages"
            into(project.layout.buildDirectory.dir(destFolder))
        }

        // Delete the python files generated building PySide6 resources
        val delGuiPy = project.tasks.register<Delete>("CleanTempGuiPy") {
            dependsOn(copyPyGuiModules)
            val srcFolder = project.layout.projectDirectory.dir("src/main/gui")
            val tree: ConfigurableFileTree = project.fileTree(srcFolder.getAsFile().getPath())

            tree.include("**/*.py")

            delete(tree)
        }

        // Standard module with scala and or java (not python only)
        // but not for python only modules that have no build task
        val buildTask = project.tasks.getByPath(":${project.name}:build")
        buildTask.finalizedBy(conf)
        buildTask.finalizedBy(pyScripts)
        buildTask.finalizedBy(shScripts)

        // Tasks for GUIs
        try {
            // distTar is not created for python only (or GUI) modules that use
            // the base plugin instead of java or scala. In this case, 
            // we add it. In case distTar exists, an exception
            // is thrown but there is nothing to do
            project.tasks.create("distTar") 
        } catch (e: Exception) {}

        
        buildTask.finalizedBy(pyside6GuiBuilder) // Build PySide6 stuff
        buildTask.finalizedBy(copyPyGuiModules)
        buildTask.finalizedBy(delGuiPy)

        buildTask.finalizedBy(copyExtLibs)
        // buildTask.finalizedBy(pyTestScripts)
        // buildTask.finalizedBy(pyTestModules)

        // val runIasUnitTestsTask = project.tasks.register<Exec>("iasUnitTest") {
        //     dependsOn(":build", pyTestScripts)
        //     onlyIf {
        //         val testFolder = project.layout.projectDirectory.dir("src/test")
        //         if (testFolder.getAsFile().exists()) {
        //             val testFile = testFolder.file("runTests.sh").getAsFile()
        //             val cond = testFile.exists()
        //             if (!cond) {
        //                 logger.warn("Test file missing {}",testFile.getPath())    
        //             }
        //             cond
        //         } else {
        //             logger.warn("Test folder missing {}",testFolder.getAsFile().getPath())
        //             false
        //         }
        //     }
        //     commandLine("src/test/runTests.sh")
        // }

        // val runIasIntTestsTask = project.tasks.register<Exec>("iasIntTest") {
        //     dependsOn(":build", pyTestScripts)
        //     onlyIf {
        //         val testFolder = project.layout.projectDirectory.dir("src/test")
        //         if (testFolder.getAsFile().exists()) {
        //             val testFile = testFolder.file("runIntTests.sh").getAsFile()
        //             val cond = testFile.exists()
        //             if (!cond) {
        //                 logger.warn("Test file missing {}",testFile.getPath())    
        //             }
        //             cond
        //         } else {
        //             logger.warn("Test folder missing {}",testFolder.getAsFile().getPath())
        //             false
        //         }
        //     }
        //     commandLine("src/test/runIntTests.sh")
        // }

        // val runIasTests = project.tasks.register("iasTest") {
        //     dependsOn(runIasUnitTestsTask)
        //     dependsOn(runIasIntTestsTask)
        // }


        project.tasks.withType<JavaCompile>().configureEach {
            options.isDeprecation = true

            val compilerArgs = options.compilerArgs
            // compilerArgs.add("-Xdoclint:all,-missing")
            compilerArgs.add("-Xlint:all")
        }

        project.tasks.withType<Jar>().configureEach {
            // Put the jars in lib
            destinationDirectory.set(project.layout.buildDirectory.dir("lib"))
            // Set the name of the jar file (overridden by the test jar task upon)
            archiveFileName.set("ias"+archiveFileName.get())
            logger.info("Will create JAR {} in {}", archiveFileName.get(), destinationDirectory.get().toString())
        }

        project.tasks.withType<ScalaCompile>().configureEach {
            scalaCompileOptions.forkOptions.apply {
                memoryMaximumSize = "1g"
            }
        }
    }
}
