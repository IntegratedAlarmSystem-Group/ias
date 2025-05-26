plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    if (g is ExtensionAware) {
      val extension = g as ExtensionAware
      implementation(extension.extra["scala-library"].toString())
      implementation(extension.extra["scalatest"].toString())
      implementation(extension.extra["scala-logging"].toString())
      implementation(extension.extra["logback-classic"].toString())
      implementation(extension.extra["jackson-databind"].toString())
      implementation(extension.extra["junit-jupiter-api"].toString())
      implementation(extension.extra["junit-jupiter-engine"].toString())
    }
    
    implementation(project(":Tools"))
    implementation(project(":Cdb"))

    if (g is ExtensionAware) {
        val extension = g as ExtensionAware
        testImplementation(extension.extra["scalatest"].toString())
        testImplementation(extension.extra["scalatestplus-junit"].toString())
        testImplementation(extension.extra["junit-jupiter-api"].toString())
        testRuntimeOnly(extension.extra["junit-jupiter-engine"].toString())
    }
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
        scala {
            setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java"))
        }
    }
}


tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

