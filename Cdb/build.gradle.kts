plugins {
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.hibernate.jpa)
    
    implementation("com.mchange:c3p0:0.9.5.2")
    implementation("org.hibernate:hibernate-c3p0:5.2.6.Final")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
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
}

tasks.named("pytest").configure {
    dependsOn(project(":BasicTypes").tasks.named("build"))
}
