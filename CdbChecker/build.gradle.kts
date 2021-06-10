plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("org.scalatest:scalatest_2.13:3.2.9")
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")

    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}


