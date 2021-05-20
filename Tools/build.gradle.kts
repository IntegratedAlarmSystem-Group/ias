plugins {
    id("scala")
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    testImplementation("org.scalatest:scalatest_2.13:3.2.5")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

base.archivesBaseName = "iasTools"
base.libsDirName ="lib"

java {
    toolchain {
        val g = project.gradle
        val jdkVersion = if (g is ExtensionAware) {
            val extension = g as ExtensionAware
            extension.extra["JdkVersion"].toString().toInt()
        } else {
            throw GradleException("Cannot determine the version of Jdk")
        }
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }


}

repositories {
    mavenCentral()
}
