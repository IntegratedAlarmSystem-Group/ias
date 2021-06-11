plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.apache.kafka:kafka-streams:2.8.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation(project(":Cdb"))
    implementation(project(":Plugin"))
    implementation(project(":Tools"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    //testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    implementation("org.apache.kafka:kafka-clients:2.8.0")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}


