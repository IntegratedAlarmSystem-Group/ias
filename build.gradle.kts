fun checkIntegrity(): Boolean {
    println("Checking integrity")
    var envVar: String? = System.getenv("ACS_ROOT")

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

//build {
//    doFirst {
//        if (!checkIntegrity()) {
//            throw StopExecutionException("Integrity check not passed")
//        } else {
//            println("Integrity check passed")
//        }
//    }
//}

//tasks.register("install") {
//    dependsOn("build")
//
//    doLast {
//        println("install!")
//    }
//}


tasks.named("build") {
    doFirst {
        if (!checkIntegrity()) {
            throw StopExecutionException("Integrity check not passed")
        } else {
            println("Integrity check passed")
        }
    }
}
