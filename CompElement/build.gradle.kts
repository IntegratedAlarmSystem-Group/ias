plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
   val g = project.gradle 
    val extension = g as ExtensionAware
    implementation(extension.extra["scala-library"].toString())
    implementation(extension.extra["scala-logging"].toString())
    implementation(extension.extra["logback-classic"].toString())
    implementation(extension.extra["jep"].toString())

    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))

    testImplementation(extension.extra["junit-jupiter"].toString())
    testImplementation(extension.extra["scalatest"].toString())
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
    
environment("PYTHONPATH", listOf(
        "/home/acaproni/ias/CompElement/src/main/python",
        "/home/acaproni/venv/lib64/python3.12/site-packages"
    ).joinToString(":"))
    environment("PYTHONHOME", "/home/acaproni/venv")
    useJUnitPlatform()
}
