plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.kafka.clients)
    implementation(libs.logback.classic)

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":CompElement"))
    implementation(project(":Cdb"))
    implementation(project(":CdbChecker"))
    implementation(project(":KafkaUtils"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}

tasks.test {
            useJUnitPlatform()
}
