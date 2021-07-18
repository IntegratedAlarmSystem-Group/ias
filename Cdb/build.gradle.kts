plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {

    val g = project.gradle
    if (g is ExtensionAware) {
      val extension = g as ExtensionAware
      implementation(extension.extra["jackson-databind"].toString())
      implementation(extension.extra["slf4j-api"].toString())
      implementation(extension.extra["logback-classic"].toString())
      implementation(extension.extra["junit-jupiter-api"].toString())
      implementation(extension.extra["junit-jupiter-engine"].toString())
      implementation(extension.extra["hibernate-jpa"].toString())
    }
    
    implementation("com.mchange:c3p0:0.9.5.2")
    implementation("org.hibernate:hibernate-c3p0:5.2.6.Final")
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


