plugins {
    base
}

import java.nio.file.StandardCopyOption
import java.nio.file.Files
import java.io.File
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test


fun checkIntegrity(): Boolean {
    var envVar: String? = System.getenv("IAS_ROOT")
    if (envVar==null) {
        return false
    }
    return true
}

tasks.named("build") {
    doFirst {
        if (!checkIntegrity()) {
            throw GradleException("Integrity check not passed: IAS_ROOT is undefined")
        }
        logger.lifecycle("Building IAS")
    }
}

tasks.register("install") {
    doFirst {
        if (!checkIntegrity()) {
            throw GradleException("Integrity check not passed: IAS_ROOT is undefined")
        }
        var envVar: String? = System.getenv("IAS_ROOT")
        logger.lifecycle("Installing IAS license, realease and readme files in $envVar")
        val extension = gradle as ExtensionAware
        val gitBranchStr = extension.extra["GitBranch"].toString()
        logger.lifecycle("Installing branch: $gitBranchStr")

        // Create the directory if it doesn't exist
        val directory = File(envVar)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "RELEASE.txt")
        file.writeText(gitBranchStr)
        // org.jetbrains.kotlin.konan.file.File(envVar + "/" + "RELEASE.txt").writeText("Built from git branch " + gitBranchStr)

        val licFile = File("LICENSE.md")
        val outLicFile = File(envVar + "/" + "LICENSE.md")
        Files.copy(licFile.toPath(), outLicFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val readmeFile = File("README.md")
        val outReadmeFile = File(envVar + "/" + "README.md")
       Files.copy(readmeFile.toPath(), outReadmeFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

subprojects {
    // Adds the pytest task to run pythin tests
    // only if module has Python tests: src/test/python exists and contains at least one *.py file
    val pythonDir = file("src/test/python")

    val hasPythonFilesTopLevel =
        pythonDir.exists() &&
        pythonDir.isDirectory &&
        (pythonDir.listFiles { f -> f.isFile && f.extension == "py" }?.isNotEmpty() == true)

    if (hasPythonFilesTopLevel) {
        val pytestTask = tasks.register<Exec>("pytest") {
            description = "Run pytest for $project"
            group = "verification"

            // If CopyPyMods exists (likely as the test probably test the modules delivered by the subproject),
            // the test depends on it
            if (tasks.names.contains("CopyPyMods")) {
                dependsOn(tasks.named("CopyPyMods"))
            }

            doFirst {
                mkdir(layout.buildDirectory.dir("test-results/pytest").get().asFile)
            }

            // Build PYTHONPATH dynamically
            val pythonPaths = mutableListOf<String>()
            pythonPaths.add(layout.buildDirectory.dir("src/test/python").get().asFile.absolutePath)

            // Include all subprojects' site-packages
            val extension = gradle as ExtensionAware
            rootProject.subprojects.forEach { sub ->
                pythonPaths.add(
                    sub.layout.buildDirectory
                        .dir("lib/${extension.extra["PythonVersion"]}/site-packages")
                        .get().asFile.absolutePath
                )
            }

            val existingPath = System.getenv("PYTHONPATH") ?: ""
            environment("PYTHONPATH", existingPath + ":" + pythonPaths.joinToString(":"))

            // Use separate directory for pytest reports
            commandLine(
                "pytest",
                "src/test/python",
                "--junitxml=${layout.buildDirectory.dir("test-results/pytest").get().asFile}/TEST-${project.name}-pytest.xml"
            )
        }

        // Attach pytest to check (check exists for modules with java, scala and python sources while
        // test task exists only for modules with java/scala sources)
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(pytestTask)
        }

    ///////////////////////////////////////////////////////////////////////////////////////////////
        
    // Only configure if the project actually uses JVM (Java/Scala)
    // Note: the 'scala' plugin applies 'java' automatically, so checking for 'java' is enough
    plugins.withId("java") {
        println(" Checking")
        // Optional: only if the folder exists
        val hasItDir = file("src/integrationTest").exists()

        // Create the 'integrationTest' source set
        val sourceSets = the<SourceSetContainer>()
        if (hasItDir) {
            val integrationTest = sourceSets.create("integrationTest") {
                // Java sources
                java.srcDir("src/integrationTest/java")
                // Resources
                resources.srcDir("src/integrationTest/resources")

                // Classpaths: main output + same deps as normal tests
                compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
                runtimeClasspath += output + compileClasspath
            }

            // Reuse the same dependencies you have for unit tests
            configurations[integrationTest.implementationConfigurationName]
                .extendsFrom(configurations["testImplementation"])
            configurations[integrationTest.runtimeOnlyConfigurationName]
                .extendsFrom(configurations["testRuntimeOnly"])

            // For Scala projects: the Scala plugin automatically recognizes 'src/integrationTest/scala'
            // — no extra wiring necessary.

            // Register the task that runs integration tests
            val integrationTestTask = tasks.register<Test>("integrationTest") {
                description = "Runs the JVM integration tests."
                group = "verification"
                testClassesDirs = integrationTest.output.classesDirs
                classpath = integrationTest.runtimeClasspath


                // Ensure integration test sources are compiled before running
                dependsOn(tasks.named(integrationTest.classesTaskName))


                // Keep the lifecycle sensible: run after unit tests if they exist
                if (tasks.names.contains("test")) {
                    shouldRunAfter(tasks.named("test"))
                }

                // JUnit platform: enable this if your project uses JUnit 5.
                // If you're still on JUnit 4 or ScalaTest (no JUnit 5 engine), leave it commented.
                useJUnitPlatform()

                reports.junitXml.required.set(true)
                reports.html.required.set(true)
            }
        }
    }


    }

    // If module has CopyPyMods, ensure assemble triggers it
    if (tasks.names.contains("CopyPyMods")) {
        tasks.named("assemble") {
            dependsOn(tasks.named("CopyPyMods"))
        }

        if (tasks.names.contains("check")) {
            tasks.named("check") {
                dependsOn(tasks.named("CopyPyMods"))
            }
        }

    }
}

val verifyTestResults = tasks.register("verifyTestResults") {
    group = "verification"
    description = "Check JUnit XML reports and list files with failures or errors"

    doLast {
        val xmlFiles = rootProject.allprojects.flatMap { p ->
            val dir = p.layout.buildDirectory.dir("test-results").get().asFile
            if (dir.exists()) {
                dir.walk().filter { it.isFile && it.extension == "xml" }.toList()
            } else {
                emptyList()
            }
        }

        val failedFiles = xmlFiles.filter { file ->
            val content = file.readText()
            content.contains("<failure") || content.contains("<error")
        }

        if (failedFiles.isNotEmpty()) {
            println("❌ Test failures detected in the following XML files:")
            failedFiles.forEach { println(" - ${it.absolutePath}") }
            throw GradleException("Some tests failed. Check reports above.")
        } else {
            println("✅ All tests passed. No failures or errors found.")
        }
    }
}



allprojects {
    // Some projects may not have a 'check' task (e.g., purely custom projects),
    // so use matching/configureEach to bind only where it exists.
    tasks.matching { it.name == "check" }.configureEach {
        finalizedBy(verifyTestResults)
    }
}


tasks.named("build") {
    finalizedBy(verifyTestResults)
}
