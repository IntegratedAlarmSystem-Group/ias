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
    implementation(extension.extra["scalatest"].toString())
    implementation(extension.extra["scala-logging"].toString())
    implementation(extension.extra["slf4j-api"].toString())
    implementation(extension.extra["junit-jupiter-api"].toString())
    implementation(extension.extra["junit-jupiter-engine"].toString())
    implementation(extension.extra["kafka-clients"].toString())
    implementation(extension.extra["logback-classic"].toString())

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
}
