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
    implementation(extension.extra["logback-classic"].toString())

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":CompElement"))
    implementation(project(":Cdb"))
    implementation(project(":CdbChecker"))
    implementation(project(":KafkaUtils"))

    testImplementation(extension.extra["junit-jupiter"].toString())
    testImplementation(extension.extra["scalatest"].toString())
}

tasks.test {
            useJUnitPlatform()
            /// Exclude integration tests
            exclude("**/DasuWithKafkaPubSubTest.class")
}
