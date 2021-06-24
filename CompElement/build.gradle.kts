plugins {
    `scala`
    `java`
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
      //implementation(extension.extra["jackson-databind"].toString())
      implementation(extension.extra["junit-jupiter-api"].toString())
      implementation(extension.extra["junit-jupiter-engine"].toString())
    }
    
    implementation("black.ninia:jep:3.9.0")

    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))
    implementation(project(":Converter"))
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
                //setSrcDirs(listOf("src/main/java"))
            }
            java {
                //setSrcDirs(listOf("src/main/java"))
                setSrcDirs( listOf<String>() )
            }
        }
    }
    test {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java"))
            }
        }
    }
}


