plugins {
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.kafka.clients)
    implementation(libs.hibernate.jpa)

    implementation(project(":Cdb"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

sourceSets {
    main {
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
    test {
        resources {
            setSrcDirs(listOf("src/test/resources"))
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
            // To cope with system properties set statically in PublisherMaxBufferSizeTest
            forkEvery = 1
}
