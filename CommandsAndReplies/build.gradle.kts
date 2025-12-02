plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    val extension = g as ExtensionAware
    implementation(extension.extra["scala-library"].toString())
    implementation(extension.extra["jackson-databind"].toString())
    implementation(extension.extra["slf4j-api"].toString())
    implementation(extension.extra["logback-classic"].toString())
    implementation(extension.extra["kafka-clients"].toString())

    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))
    implementation(project(":BasicTypes"))
    implementation(project(":Heartbeat"))

    testImplementation(extension.extra["junit-jupiter"].toString())
}

tasks.test {
            useJUnitPlatform()
            // Exclude integration tests
            exclude("**/TestCommandSender.class")
            exclude("**/TestCommandManager.class")

}

tasks.named("pytest").configure {
    dependsOn(project(":Tools").tasks.named("build"))
    dependsOn(project(":BasicTypes").tasks.named("build"))
    dependsOn(project(":KafkaUtils").tasks.named("build"))
}
