plugins {
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.commons.cli)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.logback.classic)
    implementation(libs.hibernate.jpa)
    implementation(platform(libs.junit.bom))
    implementation(libs.junit.platform.console)

    implementation(project(":Cdb"))
    implementation(project(":Plugin"))
    implementation(project(":Tools"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
            useJUnitPlatform()
}
