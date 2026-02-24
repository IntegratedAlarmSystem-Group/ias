plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.logback.classic)
    implementation(libs.commons.cli)

    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
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
