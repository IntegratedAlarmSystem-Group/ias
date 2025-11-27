plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {

    val g = project.gradle
    val extension = g as ExtensionAware
    implementation(extension.extra["jackson-databind"].toString())
    implementation(extension.extra["slf4j-api"].toString())
    implementation(extension.extra["logback-classic"].toString())
    implementation(extension.extra["hibernate-jpa"].toString())
    
    implementation("com.mchange:c3p0:0.9.5.2")
    implementation("org.hibernate:hibernate-c3p0:5.2.6.Final")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")

    testImplementation(extension.extra["junit-jupiter"].toString())
    testImplementation("org.junit.platform:junit-platform-launcher:6.0.0")
}

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}

distributions {
    main {
        contents {
            from("src/main/resources")
        }
    }
}

tasks.test {
            useJUnitPlatform()
            // Excluded because RDB has been deprecated
            exclude("**/TestRdbCdb.class")
            
            // To be moved in integration tests because it requires iasRun to build te path when 
            // IAS_EXTERNAL_JARS is set
            exclude("**/CdbReaderFactoryTest.class")
            ignoreFailures = true
}
