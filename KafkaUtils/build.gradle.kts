plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.slf4j.api)
    implementation(libs.kafka.clients)
    implementation(libs.logback.classic)

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}

tasks.test {
            useJUnitPlatform()
}
