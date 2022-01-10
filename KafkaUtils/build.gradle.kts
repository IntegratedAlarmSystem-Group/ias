plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    if (g is ExtensionAware) {
      val extension = g as ExtensionAware
      implementation(extension.extra["slf4j-api"].toString())
      implementation(extension.extra["junit-jupiter-api"].toString())
      implementation(extension.extra["junit-jupiter-engine"].toString())
      implementation(extension.extra["kafka-clients"].toString())
    }

    implementation(project(":BasicTypes"))

}
