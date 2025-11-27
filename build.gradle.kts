plugins {
    base
}

import java.nio.file.StandardCopyOption
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
        java.nio.file.Files.copy(licFile.toPath(), outLicFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val readmeFile = File("README.md")
        val outReadmeFile = File(envVar + "/" + "README.md")
        java.nio.file.Files.copy(readmeFile.toPath(), outReadmeFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

subprojects {
}
