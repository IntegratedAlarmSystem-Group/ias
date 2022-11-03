import java.nio.file.StandardCopyOption

fun checkIntegrity(): Boolean {
    var envVar: String? = System.getenv("IAS_ROOT")
    if (envVar==null) {
        return false
    }
    return true
}

tasks.register("build") {
    doFirst {
        if (!checkIntegrity()) {
            throw GradleException("Integrity check not passed: IAS_ROOT is undefined")
        }
        logger.lifecycle("Building IAS")
    }
}

tasks.register("install") {
    var envVar: String? = System.getenv("IAS_ROOT")
    val extension = gradle as ExtensionAware
    val gitBranchStr = extension.extra["GitBranch"].toString()
    org.jetbrains.kotlin.konan.file.File(envVar + "/" + "RELEASE.txt").writeText("Built from git branch " + gitBranchStr)

    val licFile = File("LICENSE.md")
    val outLicFile = File(envVar + "/" + "LICENSE.md")
    java.nio.file.Files.copy(licFile.toPath(), outLicFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    val readmeFile = File("README.md")
    val outReadmeFile = File(envVar + "/" + "README.md")
    java.nio.file.Files.copy(readmeFile.toPath(), outReadmeFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

subprojects {
}
