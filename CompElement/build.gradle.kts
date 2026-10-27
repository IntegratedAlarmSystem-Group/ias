import java.io.File

plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.logback.classic)

    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}

sourceSets {
    main {
        scala {
            setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
        }
        java {
            setSrcDirs( listOf<String>() )
        }
    }
    test {
        java {
            setSrcDirs(listOf("src/test/java")) // explicitly include Java
        }

        scala {
            setSrcDirs(listOf("src/test/scala"))
        }
    }
}

tasks.test {
    val extension = gradle as ExtensionAware
    val pythonVersion = extension.extra["PythonVersion"] as String

    val pythonRoot = System.getenv("Python_ROOT_DIR")
    // JEP removed – no longer need java.library.path for libjep.so

    // Always use absolute paths; Gradle may execute tests from the root project dir
    val compElementProject = project(":CompElement")
    val basicTypeProject = project(":BasicTypes")
    val toolsProject = project(":Tools")

    val buildSitePackages =  compElementProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val builBasicTypesPython = basicTypeProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val buildToolsPython = toolsProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val srcTestPython      = compElementProject.file("src/test/python").absolutePath

    // Prefer the built package directory first
    val pythonPath = listOf(buildSitePackages, builBasicTypesPython, buildToolsPython, srcTestPython)
        .joinToString(File.pathSeparator)

    environment("PYTHONPATH", pythonPath)
    print("PYTHONPATH $pythonPath")

    // Skip Python TF tests – PythonExecutorTF is a stub until gRPC replacement (Phase 2)
    exclude("org/eso/ias/asce/test/TestPyTF*.class")
    exclude("org/eso/ias/asce/test/TestMinMaxPyTF*.class")

    useJUnitPlatform()

    doFirst {
        println(">>> Python_ROOT_DIR=$pythonRoot")
        println(">>> PYTHONPATH=$pythonPath")
        println(">>> JAVA_HOME=${System.getenv("JAVA_HOME")}")
        println(">>> LD_LIBRARY_PATH=${System.getenv("LD_LIBRARY_PATH")}")
        println(">>> java.library.path=${System.getProperty("java.library.path")}")
    }
}
