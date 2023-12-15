package org.eso.ias.build.plugin

import java.io.File as JavaFile

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.konan.file.File

open class IasBuild : Plugin<Project> {
    override fun apply(project: Project) {

        val logger = project.getLogger()
        logger.info("IAS build plugin in {}", project.buildDir)

        // Get the python version from the property in settings.gradle.kts
        val g = project.gradle
        val pythonVersion = if (g is ExtensionAware) {
            val extension = g as ExtensionAware
            extension.extra["PythonVersion"]
        } else {
            throw GradleException("Cannot determine the version of python3")
        }
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
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
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
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
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
        }

        val installBin = project.tasks.register<Copy>("InstallBin") {
            var envVar: String? = System.getenv("IAS_ROOT")

            dependsOn(":${project.name}:CopyPyScripts")
            dependsOn(":${project.name}:CopyShScripts")
            dependsOn(":${project.name}:CopyPyTestScripts")

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
        val pyTestScripts = project.tasks.register<Copy>("CopyPyTestScripts") {
            val pyFolder = "src/test/python"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
        }

        // Copy python test modules
        // i.e. each folder in src/test/python
        val pyTestModules = project.tasks.register<Copy>("CopyPyTestMods") {
            val srcFolder = "src/test/python"

            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")
            val destFolder = "lib/python${pythonVersion}/site-packages"
            into(project.layout.buildDirectory.dir(destFolder))
        }

        try {
            // Standard module with scala and or java (not python only)
            // but not for python only modules that have no build task
            val buildTask = project.tasks.getByPath(":${project.name}:build")

            buildTask.finalizedBy(conf)
            buildTask.finalizedBy(pyScripts)
            buildTask.finalizedBy(shScripts)
            buildTask.finalizedBy(pyModules)
            buildTask.finalizedBy(copyExtLibs)
            buildTask.finalizedBy(pyTestScripts)
            buildTask.finalizedBy(pyTestModules)

        } catch (e: Exception) {
            // If there is no build then it is a python only module 
            // including a module with PySide6 GUI only
            logger.info("Build task not found")
            
            project.tasks.create("build")
            project.tasks.create("distTar")

            val buildTask = project.tasks.getByPath(":${project.name}:build")
            buildTask.finalizedBy(conf)
            buildTask.finalizedBy(pyScripts)
            buildTask.finalizedBy(shScripts)
            buildTask.finalizedBy(pyModules)
            buildTask.finalizedBy(pyTestScripts)
            buildTask.finalizedBy(pyTestModules)
        }
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(conf)
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(pyScripts)
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(shScripts)
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(pyModules)
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(copyExtLibs)
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(pyTestScripts)
        // project.tasks.getByPath(":${project.name}:build").finalizedBy(pyTestModules)

        val runIasTestsTask = project.tasks.register<Exec>("iasTest") {
            dependsOn(":build", pyTestScripts)
            commandLine("src/test/runTests.sh")
        }


        project.tasks.withType<JavaCompile>().configureEach {
            options.isDeprecation = true

            val compilerArgs = options.compilerArgs
            // compilerArgs.add("-Xdoclint:all,-missing")
            compilerArgs.add("-Xlint:all")

            val extension = g as ExtensionAware
            val jdkVersion = extension.extra["JdkVersion"].toString().toInt()
            options.release.set(jdkVersion)
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
