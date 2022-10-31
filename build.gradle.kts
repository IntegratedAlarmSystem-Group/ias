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
}

subprojects {
}
