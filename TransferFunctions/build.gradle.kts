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

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":CompElement"))

    testImplementation(extension.extra["junit-jupiter"].toString())
    testImplementation(extension.extra["scalatest"].toString())
}

tasks.test {
            useJUnitPlatform()
}
