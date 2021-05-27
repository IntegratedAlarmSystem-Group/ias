
fun checkIntegrity(): Boolean {
    println("Checking integrity")
    var envVar: String? = System.getenv("IAS_ROOT")

    if (envVar==null) {
        return false
    }

    return true
}


tasks.register("build") {
    doLast {
        println("Building")
    }
}

tasks.named("build") {
    doFirst {
        if (!checkIntegrity()) {
            throw StopExecutionException("Integrity check not passed")
        } else {
            println("Integrity check passed")
        }
    }
}

repositories {
    mavenCentral()
}
