package org.eso.ias.build.plugin

import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register

open class IasBuild : Plugin<Project> {

    override fun apply(project: Project) {
        println("IAS build plugin in ${project.buildDir}")

        // Get the python version from the property in settings.gradle.kts
        val g = project.gradle
        val pythonVersion = if (g is ExtensionAware) {
            val extension = g as ExtensionAware
            extension.extra["PythonVersion"]
        } else {
            throw GradleException("Cannot determine the version of python3")
        }
        println("Using python version $pythonVersion")

        // Configurations
        val conf: TaskProvider<Copy> = project.tasks.register<Copy>("CopyConfigFiles") {
            doFirst {
                println("CopyConfigs doFirst")
            }

            val confFolder = "config"
            val destFolder = "config"

            println("Configuring: Installing configuration files in ${project.buildDir}")
            from(project.layout.projectDirectory.dir(confFolder))
            include("*")
            into(project.layout.buildDirectory.dir(destFolder))
            println("Configured: Installing configuration files in ${project.buildDir}")

            doLast {
                println("CopyConfigs doLast")
            }
        }

        // Python Scripts
        val pyScripts: TaskProvider<Copy> = project.tasks.register<Copy>("CopyPyScripts") {

            doFirst {
                println("CopyPyScripts doFirst")
            }

            println("Configuring: Installing python scripts in ${project.buildDir}")
            val pyFolder = "src/main/python"
            val destFolder = "bin"


            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            println("Configured: Installing python scripts in ${project.buildDir}")

            doLast {
                println("CopyPyScripts doLast")
            }

        }

        // Shell scripts
        val shScripts: TaskProvider<Copy> = project.tasks.register<Copy>("CopyShScripts") {
            doFirst {
                println("CopyShellScripts doFirst")
            }

            println("Configuring: Installing shell scripts in ${project.buildDir}")
            val shFolder = "src/main"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(shFolder))
            include("*.sh")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            println("Configured: Installing shell scripts in ${project.buildDir}")

            doLast {
                println("CopyShellScripts doLast")
            }
        }


        // Spawn one copy task task for each python modules
        // i.e. each folder in src/main/python
        val pyModules: TaskProvider<Copy> = project.tasks.register<Copy>("CopyPyMods") {

            doFirst {

                println("CopyPyMods doFirst")

            }

            println("Configuring: Installing Python modules")

            val srcFolder = "src/main/python"

            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")
            val destFolder = "lib/python${pythonVersion}/site-packages"
            into(project.layout.buildDirectory.dir(destFolder))

            println("Configured: Installing Python modules")

            doLast {
                println("CopyPyMods doLast")
            }
        }

        // Build the jar with the test classes
        // The name of the jar is built by appending "Test"to the name of the jar built by java/scala
        val buildTestJar: TaskProvider<Jar> = project.tasks.register<Jar>("buildJarOfTestClasses") {
            from(project.layout.buildDirectory.dir("classes/scala/test"))
            from(project.layout.buildDirectory.dir("java/scala/test"))
            destinationDirectory.set(project.layout.buildDirectory.dir("lib"))
            archiveFileName.set(archiveBaseName.get()+"Test.jar")
            doFirst {
                println("buildJarOfTestClasses doFirst")
            }
            doLast {
                println("buildJarOfTestClasses doLast")
            }
        }

        project.tasks.getByPath(":${project.name}:build").finalizedBy(conf)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(shScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyModules)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(buildTestJar)
    }
}