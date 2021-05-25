plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.9.4")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")

    implementation(project(":Cdb"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.apache.kafka:kafka-clients:2.8.0")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}

repositories {
    mavenCentral()
}


