plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))

}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
   mavenCentral()
}


