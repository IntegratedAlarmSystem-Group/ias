plugins {
    `java`
    `java-library-distribution`
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.apache.kafka:kafka-clients:2.8.0")

    implementation(project(":BasicTypes"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")

}

base.archivesBaseName = "iasKafkaUtils"

repositories {
    mavenCentral()
}


