plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation(project(":Tools"))
    implementation(project(":KafkaUtils"))
    implementation(project(":BasicTypes"))
    implementation(project(":Heartbeat"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")

}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}


