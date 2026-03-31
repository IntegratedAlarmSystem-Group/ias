plugins {
    scala
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.commons.cli)
    implementation(libs.logback.classic)
    implementation(libs.kafka.clients)
    implementation(libs.scalatest)

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":DistributedUnit"))
    implementation(project(":Heartbeat"))
    implementation(project(":CdbChecker"))
    implementation(project(":KafkaUtils"))

    testImplementation(project(":CompElement"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)

}

tasks.test {
            useJUnitPlatform()
}
