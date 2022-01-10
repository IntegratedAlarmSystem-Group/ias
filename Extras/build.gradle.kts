plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    if (g is ExtensionAware) {
      val extension = g as ExtensionAware
      implementation(extension.extra["scala-library"].toString())
      implementation(extension.extra["scala-logging"].toString())
    }

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))

}

repositories {
   mavenCentral()
}


