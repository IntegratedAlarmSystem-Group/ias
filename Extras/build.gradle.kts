plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    val extension = g as ExtensionAware
    implementation(extension.extra["scala-library"].toString())
    implementation(extension.extra["scala-logging"].toString())
    implementation(extension.extra["kafka-clients"].toString())
    implementation(extension.extra["commons-cli"].toString())
    implementation(extension.extra["logback-classic"].toString())

    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
}
