package org.eso.ias.build.plugin

import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.*

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
        val conf = project.tasks.register<Copy>("CopyConfigFiles") {
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
        val pyScripts = project.tasks.register<Copy>("CopyPyScripts") {

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
        val shScripts = project.tasks.register<Copy>("CopyShScripts") {
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
        val pyModules = project.tasks.register<Copy>("CopyPyMods") {

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
        val buildTestJar = project.tasks.register<Jar>("buildJarOfTestClasses") {
            from(project.layout.buildDirectory.dir("classes/scala/test"))
            from(project.layout.buildDirectory.dir("classes/java/test"))
            destinationDirectory.set(project.layout.buildDirectory.dir("lib"))
            archiveFileName.set(archiveBaseName.get()+"Test.jar")
            doFirst {
                println("${project.name}: buildJarOfTestClasses doFirst")
            }
            doLast {
                println("${project.name}: buildJarOfTestClasses doLast")
            }
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

            doFirst {
                println("Installing bins of ${project.name}")
            }
            doLast {
                println("Installed bins of ${project.name}")
            }
        }

        val installConfig = project.tasks.register<Copy>("InstallConfig") {
            var envVar: String? = System.getenv("IAS_ROOT")

            dependsOn(":${project.name}:CopyConfigFiles")

            from(project.layout.buildDirectory.dir("config"))
            include("**/*")
            val destFolder = "${envVar}/config"
            into(destFolder)

            doFirst {
                println("Installing configs of ${project.name}")
            }
            doLast {
                println("Installed configs of ${project.name}")
            }
        }

        val installLib = project.tasks.register<Copy>("InstallLib") {
            var envVar: String? = System.getenv("IAS_ROOT")

            dependsOn(":${project.name}:CopyExtLib")
            dependsOn(":${project.name}:CopyPyMods")

            from(project.layout.buildDirectory.dir("lib"))
            include("**/*")
            exclude("*Test.jar")
            val destFolder = "${envVar}/lib"
            into(destFolder)

            doFirst {
                println("Installing libs of ${project.name}")
            }
            doLast {
                println("Installed libs of ${project.name}")
            }
        }

        project.tasks.register("install") {
            dependsOn(installConfig)
            dependsOn(installBin)
            dependsOn(installLib)

            var envVar: String? = System.getenv("IAS_ROOT")
            if (envVar==null) {
                throw GradleException("IAS_ROOT undefined")
            }

            doFirst {
                println("Installing ${project.name}")
            }
            doLast {
                println("Installed ${project.name}")
            }
        }

        // Untar the archive in build/distribution
        val untar = project.tasks.register<Copy>("Untar") {
            dependsOn(":${project.name}:distTar")
            val folder = project.layout.buildDirectory.dir("distributions")
            val tarFileName = folder.get().asFile.path+"/${project.name}.tar"
            println("Configuring ${project.name}:untar for $tarFileName")
            from(project.tarTree(tarFileName))
            into(folder)
            doFirst {
                println("Untar ${project.name} begin ")
            }
            doLast {
                println("Untar ${project.name} done")
            }
        }

        val copyExtLibs = project.tasks.register<Copy>("CopyExtLib") {
            dependsOn(untar)
            println("Configuring ${project.name}:CopyExtLib")
            from(project.layout.buildDirectory.dir("distributions/${project.name}/lib"))
            into(project.layout.buildDirectory.dir("lib/ExtLibs"))
            exclude("ias*.jar")
            doFirst {
                println("CopyExtLib of ${project.name} begin ")
            }
            doLast {
                println("CopyExtLib of ${project.name} done")
            }
        }

        // Copy python test scripts in build/bin
        val pyTestScripts = project.tasks.register<Copy>("CopyPyTestScripts") {

            doFirst {
                println("${project.name}: CopyPyTestScripts doFirst")
            }

            println("${project.name}: Configuring: Installing python test scripts in ${project.buildDir}")
            val pyFolder = "src/test/python"
            val destFolder = "bin"


            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            println("${project.name}: Configured: Installing test python scripts in ${project.buildDir}")

            doLast {
                println("${project.name}: CopyPyTestScripts doLast")
            }

        }

        project.tasks.getByPath(":${project.name}:build").finalizedBy(conf)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(shScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyModules)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(buildTestJar)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(copyExtLibs)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyTestScripts)

        val runIasTestsTask = project.tasks.register<Exec>("iasTest") {
            dependsOn(":build", pyTestScripts)
            commandLine("src/test/runTests.sh")
        }
    }



}