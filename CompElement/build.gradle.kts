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
    implementation(libs.jep)

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

    // Sets the PYTHONPATH for the python code to run with jep

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

    // 1) Make Python modules visible to JEP
    environment("PYTHONPATH", pythonPath)
    print("PYTHONPATH $pythonPath")

    // 2) Ensure the JVM can load libjep.so (adjust to your jep install dir)
    // If you already set this globally, you can skip it here.
    // val jepNativeDir = "/home/acaproni/.local/lib/python3.12/site-packages/jep"
    // systemProperty("java.library.path", jepNativeDir)

    // 3) Put jep.jar on the classpath (if you don’t use a Maven dependency)
    // Prefer a dependency (e.g., compileOnly/testImplementation from a repo),
    // but this local file approach works:
    // classpath += files("$jepNativeDir/jep.jar")

    useJUnitPlatform()

    // Optional: print the effective PYTHONPATH for debugging
    doFirst {
        println(">>> PYTHONPATH for tests: $pythonPath")
        println(">>> java.library.path: ${systemProperties["java.library.path"]}")
        println(">>> Python version: $pythonVersion")
    }
}
