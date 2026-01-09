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
    implementation(extension.extra["logback-classic"].toString())
    implementation(extension.extra["commons-cli"].toString())

    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))

    testImplementation(extension.extra["junit-jupiter"].toString())
    testImplementation(extension.extra["scalatest"].toString())
}

distributions {
    main {
        // Set the name of the tar/zip files in build
        distributionBaseName.set(project.name)
    }
}

tasks.test {
            useJUnitPlatform()
}
