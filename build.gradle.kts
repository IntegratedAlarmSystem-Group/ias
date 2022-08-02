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

subprojects {
}
