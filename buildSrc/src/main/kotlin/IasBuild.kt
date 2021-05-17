package org.eso.ias.build.plugin

import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import java.io.ByteArrayOutputStream
import java.util.Scanner

open class IasBuild : Plugin<Project> {

    override fun apply(project: Project) {
        println("IAS build plugin in ${project.buildDir}")

        // Get the version of python by running python3 -V
        // The version of python is stored in #pythonFolder
        val runPython= project.tasks.register<Exec>("RunPythonTask") {
            doFirst {
                println("Getting python version")
            }

            println("Setting extra pythonFolder property")
            val out: ByteArrayOutputStream = ByteArrayOutputStream()
            setStandardOutput(out)
            commandLine("python3", "-V")

            doLast {
                val pyVersionStr = standardOutput.toString()
                println("out ===> $pyVersionStr")
                val pyFullVersion = pyVersionStr.drop(pyVersionStr.lastIndexOf(' ')+1)
                val pyVersion = pyFullVersion.dropLast(pyFullVersion.lastIndexOf('.'))

                // This variable is set by the runPython task
                // It is like lib/pythonX.Y/site-packages
                print("Setting extra pythonFolder variable")
                extra["pythonFolder"] = "lib/python$pyVersion/site-packages"
                print("pythonFolder variable set")
                println("Got python version [${extra["pythonFolder"]}]")
            }
        }

        // Configurations
        val conf: TaskProvider<Copy> = project.tasks.register<Copy>("CopyConfigFiles") {
            doFirst {
                println("CopyConfigs doFirst")
            }

            val confFolder = "config"
            val destFolder = "config"

            from(project.layout.projectDirectory.dir(confFolder))
            include("*")
            into(project.layout.buildDirectory.dir(destFolder))
            println("Installing configuration files in ${project.buildDir}")

            doLast {
                println("CopyConfigs doLast")
            }
        }

        // Python Scripts
        val pyScripts: TaskProvider<Copy> = project.tasks.register<Copy>("CopyPyScripts") {

            doFirst {
                println("CopyPyScripts doFirst")
            }
            val pyFolder = "src/main/python"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            println("Installing python scripts in ${project.buildDir}")


            doLast {
                println("CopyPyScripts doLast")
            }

        }

        // Shell scripts
        val shScripts: TaskProvider<Copy> = project.tasks.register<Copy>("CopyShScripts") {
            doFirst {
                println("CopyShellScripts doFirst")
            }

            val shFolder = "src/main"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(shFolder))
            include("*.sh")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            println("Installing shell scripts in ${project.buildDir}")

            doLast {
                println("CopyShellScripts doLast")
            }
        }


        // Spawn one copy task task for each python modules
        // i.e. each folder in src/main/python
        val pyModules: TaskProvider<Copy> = project.tasks.register<Copy>("CopyPyMods") {
            // Depends on the follwoing python task that sets the library to store
            // the modules into
            dependsOn(runPython)
            doFirst {

                println("CopyPyMods doFirst")
            }

            val srcFolder = "src/main/python"

            val destFolder = project.extra.get("RunPythonTask.pythonFolder").toString()
            println("Install folder: ${destFolder}")


            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")

            into(project.layout.buildDirectory.dir(destFolder))
            println("Installing Python modules")

            doLast {
                println("CopyPyMods doLast")
            }
        }

        project.tasks.getByPath(":${project.name}:build").finalizedBy(conf)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(shScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(runPython)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyModules)




    }
}