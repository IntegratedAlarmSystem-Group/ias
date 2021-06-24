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
      implementation(extension.extra["jackson-databind"].toString())
      implementation(extension.extra["slf4j-api"].toString())
      implementation(extension.extra["logback-classic"].toString())
      implementation(extension.extra["commons-cli"].toString())
      implementation(extension.extra["junit-jupiter-api"].toString())
      implementation(extension.extra["junit-jupiter-engine"].toString())
    }
    implementation("org.eclipse.jetty.websocket:websocket-api:9.4.15.v20190215")
    implementation("org.eclipse.jetty.websocket:websocket-client:9.4.15.v20190215")

    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
    implementation(project(":CommandsAndReplies"))

    testImplementation("org.eclipse.jetty.websocket:websocket-server:9.4.15.v20190215")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}


