plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kafka.clients)
    implementation(platform(libs.junit.bom))
    implementation(libs.junit.platform.console)

    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))
    implementation(project(":BasicTypes"))
    implementation(project(":Heartbeat"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("pytest").configure {
    dependsOn(project(":Tools").tasks.named("build"))
    dependsOn(project(":BasicTypes").tasks.named("build"))
    dependsOn(project(":KafkaUtils").tasks.named("build"))
}
