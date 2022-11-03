plugins {
    `scala`
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
      implementation(extension.extra["commons-cli"].toString())
      implementation(extension.extra["logback-classic"].toString())
      implementation(extension.extra["kafka-clients"].toString())
    }

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":DistributedUnit"))
    implementation(project(":Heartbeat"))
    implementation(project(":CdbChecker"))
    implementation(project(":KafkaUtils"))

    testImplementation(project(":CompElement"))
}
