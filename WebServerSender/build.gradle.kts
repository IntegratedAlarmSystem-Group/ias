plugins {
    `scala`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("org.eclipse.jetty.websocket:websocket-api:9.4.15.v20190215")
    implementation("org.eclipse.jetty.websocket:websocket-client:9.4.15.v20190215")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")


    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
    implementation(project(":CommandsAndReplies"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")
    testImplementation("org.eclipse.jetty.websocket:websocket-server:9.4.15.v20190215")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}


