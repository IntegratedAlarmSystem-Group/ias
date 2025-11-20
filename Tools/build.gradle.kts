plugins {
    id("scala")
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    val g = project.gradle
    val extension = g as ExtensionAware
    implementation(extension.extra["scala-library"].toString())
    implementation(extension.extra["scalatest"].toString())
    implementation(extension.extra["slf4j-api"].toString())
    implementation(extension.extra["scala-logging"].toString())
    implementation(extension.extra["logback-classic"].toString())
    implementation(extension.extra["junit-platform-console-standalone"].toString())
    implementation("com.h2database:h2:2.1.214")
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
