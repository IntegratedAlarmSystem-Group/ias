plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.kafka.clients)
    implementation(libs.commons.cli)
    implementation(libs.logback.classic)

    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
}
