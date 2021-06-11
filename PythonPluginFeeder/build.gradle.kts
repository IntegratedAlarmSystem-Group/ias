plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")

    implementation(project(":Plugin"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":CommandsAndReplies"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}
