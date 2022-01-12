package org.eso.ias.build.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

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
            doFirst {
                logger.info("CopyConfigs doFirst")
            }

            val confFolder = "config"
            val destFolder = "config"

            logger.lifecycle("Configuring: Installing configuration files in {}", project.buildDir)
            from(project.layout.projectDirectory.dir(confFolder))
            include("*")
            into(project.layout.buildDirectory.dir(destFolder))
            logger.lifecycle("Configured: Installing configuration files in {}", project.buildDir)

            doLast {
                logger.info("CopyConfigs doLast")
            }
        }

        // Python Scripts
        val pyScripts = project.tasks.register<Copy>("CopyPyScripts") {
            doFirst {
                logger.info("CopyPyScripts doFirst")
            }

            logger.lifecycle("Configuring: Installing python scripts in {}", project.buildDir)
            val pyFolder = "src/main/python"
            val destFolder = "bin"


            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            logger.lifecycle("Configured: Installing python scripts in {}", project.buildDir)

            doLast {
                logger.info("CopyPyScripts doLast")
            }

        }

        // Shell scripts
        val shScripts = project.tasks.register<Copy>("CopyShScripts") {
            doFirst {
                logger.info("CopyShellScripts doFirst")
            }

            logger.lifecycle("Configuring: Installing shell scripts in {}", project.buildDir)
            val shFolder = "src/main"
            val destFolder = "bin"

            from(project.layout.projectDirectory.dir(shFolder))
            include("*.sh")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            logger.lifecycle("Configured: Installing shell scripts in {}", project.buildDir)

            doLast {
                logger.info("CopyShellScripts doLast")
            }
        }


        // Spawn one copy task task for each python modules
        // i.e. each folder in src/main/python
        val pyModules = project.tasks.register<Copy>("CopyPyMods") {

            doFirst {
                logger.info("CopyPyMods doFirst")
            }

            logger.lifecycle("Configuring: Installing Python modules")

            val srcFolder = "src/main/python"

            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")
            val destFolder = "lib/python${pythonVersion}/site-packages"
            into(project.layout.buildDirectory.dir(destFolder))

            logger.lifecycle("Configured: Installing Python modules")

            doLast {
                logger.info("CopyPyMods doLast")
            }
        }

        // Build the jar with the test classes
        // The name of the jar is built by appending "Test"to the name of the jar built by java/scala
        val buildTestJar = project.tasks.register<Jar>("buildJarOfTestClasses") {
            from(project.layout.buildDirectory.dir("classes/scala/test"))
            from(project.layout.buildDirectory.dir("classes/java/test"))
            from(project.layout.buildDirectory.dir("resources/test"))
            doFirst {
                // Overrides the name of the jar
                // For non test jar name see the Jar task at the bottom
                archiveFileName.set("ias"+archiveBaseName.get()+"Test.jar")
                logger.info("{}: buildJarOfTestClasses doFirst", project.name)
            }
            doLast {
                logger.info("{}: buildJarOfTestClasses doLast", project.name)
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
                logger.info("Installing bins of {}", project.name)
            }
            doLast {
                logger.info("Installed bins of ", project.name)
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
                logger.info("Installing configs of {}", project.name)
            }
            doLast {
                logger.info("Installed configs of {}", project.name)
            }
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

            doFirst {
                logger.info("Installing libs of {}", project.name)
            }
            doLast {
                logger.info("Installed libs of {}", project.name)
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
                logger.info("Installing {}", project.name)
            }
            doLast {
                logger.info("Installed {}", project.name)
            }
        }

        // Untar the archive in build/distribution
        val untar = project.tasks.register<Copy>("Untar") {
            dependsOn(":${project.name}:distTar")
            val folder = project.layout.buildDirectory.dir("distributions")
            val tarFileName = folder.get().asFile.path+"/${project.name}.tar"
            logger.lifecycle("Configuring {}:untar for {}", project.name, tarFileName)
            from(project.tarTree(tarFileName))
            into(folder)
            doFirst {
                logger.info("Untar {} begin ", project.name)
            }
            doLast {
                logger.info("Untar {} done", project.name)
            }
        }

        val copyExtLibs = project.tasks.register<Copy>("CopyExtLib") {
            dependsOn(untar)
            logger.lifecycle("Configuring {}:CopyExtLib", project.name)
            from(project.layout.buildDirectory.dir("distributions/${project.name}/lib"))
            into(project.layout.buildDirectory.dir("lib/ExtLibs"))
            exclude("ias*.jar")
            doFirst {
                logger.info("CopyExtLib of {} begin ", project.name)
            }
            doLast {
                logger.info("CopyExtLib of {} done", project.name)
            }
        }

        // Copy python test scripts in build/bin
        val pyTestScripts = project.tasks.register<Copy>("CopyPyTestScripts") {

            doFirst {
                logger.info("{}: CopyPyTestScripts doFirst", project.name)
            }

            logger.lifecycle("{}: Configuring: Installing python test scripts in {}", project.name, project.buildDir)
            val pyFolder = "src/test/python"
            val destFolder = "bin"


            from(project.layout.projectDirectory.dir(pyFolder))
            include("*.py")
            rename { filename: String ->
                filename.substringBefore('.')
            }
            fileMode = 484 // 0744
            into(project.layout.buildDirectory.dir(destFolder))
            logger.lifecycle("{}: Configured: Installing test python scripts in {}", project.name, project.buildDir)

            doLast {
                logger.info("{}: CopyPyTestScripts doLast", project.name)
            }

        }

        // Copy python test modules
        // i.e. each folder in src/test/python
        val pyTestModules = project.tasks.register<Copy>("CopyPyTestMods") {

            doFirst {
                logger.info("CopyPyTestMods doFirst")
            }

            logger.lifecycle("Configuring: Installing Python modules for testing")

            val srcFolder = "src/test/python"

            from(project.layout.projectDirectory.dir(srcFolder))
            include("**/*.py")
            exclude("*.py")
            val destFolder = "lib/python${pythonVersion}/site-packages"
            into(project.layout.buildDirectory.dir(destFolder))

            logger.lifecycle("Configured: Installing Python modules for testing")

            doLast {
                logger.info("CopyPyTestMods doLast")
            }
        }

        project.tasks.getByPath(":${project.name}:build").finalizedBy(conf)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(shScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyModules)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(buildTestJar)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(copyExtLibs)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyTestScripts)
        project.tasks.getByPath(":${project.name}:build").finalizedBy(pyTestModules)

//        project.tasks.getByPath(":${project.name}:test").dependsOn(project.tasks.getByPath(":${project.name}:build"))
//        project.tasks.getByPath(":${project.name}:test").dependsOn(project.tasks.getByPath(":${project.name}:build"))

        val runIasTestsTask = project.tasks.register<Exec>("iasTest") {
            dependsOn(":build", pyTestScripts)
            commandLine("src/test/runTests.sh")
        }
        project.tasks.withType<JavaCompile>().configureEach {
            options.isDeprecation = true

            val extension = g as ExtensionAware
            val jdkVersion = extension.extra["JdkVersion"].toString().toInt()
            options.release.set(jdkVersion)
        }

        project.tasks.withType<Jar>().configureEach {
            // Put the jars in lib
            destinationDirectory.set(project.layout.buildDirectory.dir("lib"))
            // Set the name of the jar file (overridded by the test jar task upon)
            archiveFileName.set("ias"+archiveBaseName.get()+".jar")
            logger.info("Will create JAR {} in {}", archiveFileName.get(), destinationDirectory.get().toString())
        }

        project.tasks.withType<ScalaCompile>().configureEach {
            scalaCompileOptions.forkOptions.apply {
                memoryMaximumSize = "1g"
            }
        }

    }
}
