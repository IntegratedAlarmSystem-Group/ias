plugins {
    id("scala")
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    val g = project.gradle
    if (g is ExtensionAware) {
      val extension = g as ExtensionAware
      implementation(extension.extra["scala-library"].toString())
      implementation(extension.extra["scalatest"].toString())
      implementation(extension.extra["slf4j-api"].toString())
      implementation(extension.extra["scala-logging"].toString())
      implementation(extension.extra["logback-classic"].toString())
      implementation(extension.extra["junit-platform-console-standalone"].toString())
    }
}

java {
    toolchain {
        val g = project.gradle
        val jdkVersion = if (g is ExtensionAware) {
            val extension = g as ExtensionAware
            extension.extra["JdkVersion"].toString().toInt()
        } else {
            throw GradleException("Cannot determine the version of Jdk")
        }
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}

sourceSets {
    main {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/main/scala") + listOf("src/main/java"))
            }
            java {
                setSrcDirs(listOf<String>())
            }
        }
    }
    test {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/test/scala") + listOf("src/test/java"))
            }
        }
    }
}

repositories {
    mavenCentral()
}
