plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.5")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.5")
    implementation("com.mchange:c3p0:0.9.5.2")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.hibernate:hibernate-c3p0:5.2.6.Final")

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")

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


