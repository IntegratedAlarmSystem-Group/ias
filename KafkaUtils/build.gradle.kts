plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.apache.kafka:kafka-clients:2.8.0")

    implementation(project(":BasicTypes"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")

}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}


