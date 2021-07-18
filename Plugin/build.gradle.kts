plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    if (g is ExtensionAware) {
      val extension = g as ExtensionAware
      implementation(extension.extra["scala-library"].toString())
      implementation(extension.extra["jackson-databind"].toString())
      implementation(extension.extra["slf4j-api"].toString())
      implementation(extension.extra["kafka-clients"].toString())
      implementation(extension.extra["junit-jupiter-api"].toString())
      implementation(extension.extra["junit-jupiter-engine"].toString())
      implementation(extension.extra["hibernate-jpa"].toString())
    }

    implementation(project(":Cdb"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}

repositories {
    mavenCentral()
}


