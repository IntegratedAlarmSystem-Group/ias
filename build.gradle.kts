plugins {
    base
}

import java.nio.file.StandardCopyOption
import java.nio.file.Files
import java.io.File

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
    // If module has Python tests
    if (file("src/test/python").exists()) {
        val pytestTask = tasks.register<Exec>("pytest") {
            description = "Run pytest for $project"
            group = "verification"

            // Run after Java/Scala tests only if 'test' task exists
            if (tasks.names.contains("test")) {
                mustRunAfter(tasks.named("test"))
            } else {
                // Otherwise, run after 'assemble'
                mustRunAfter(tasks.named("assemble"))
            }

            // Ensure pytest runs after assemble (ordering only)
            //mustRunAfter(tasks.named("assemble"))

            // If CopyPyMods exists, order after it
            if (tasks.names.contains("CopyPyMods")) {
                mustRunAfter(tasks.named("CopyPyMods"))
            }

            doFirst {
                mkdir(layout.buildDirectory.dir("test-results/pytest").get().asFile)
            }

            // Build PYTHONPATH dynamically
            val pythonPaths = mutableListOf<String>()
            pythonPaths.add(layout.buildDirectory.dir("src/test/python").get().asFile.absolutePath)

            // Include all subprojects' site-packages
            rootProject.subprojects.forEach { sub ->
                pythonPaths.add(
                    sub.layout.buildDirectory
                        .dir("lib/python${gradle.extra["PythonVersion"]}/site-packages")
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

        // Attach pytest to check
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(pytestTask)
        }
    }

    // If module has CopyPyMods, ensure assemble triggers it
    if (tasks.names.contains("CopyPyMods")) {
        tasks.named("assemble") {
            dependsOn(tasks.named("CopyPyMods"))
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
