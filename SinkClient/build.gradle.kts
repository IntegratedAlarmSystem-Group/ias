plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    val extension = g as ExtensionAware
    implementation(extension.extra["scala-library"].toString())
    //implementation(extension.extra["scalatest"].toString())
    implementation(extension.extra["scala-logging"].toString())
    implementation(extension.extra["logback-classic"].toString())
    implementation(extension.extra["kafka-clients"].toString())
    implementation(extension.extra["commons-cli"].toString())
    implementation(extension.extra["kafka-connect-api"].toString())
    
    implementation("com.datastax.cassandra:cassandra-driver-core:3.6.0")
    implementation("javax.mail:mail:1.4.7")

    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":DistributedUnit"))
    implementation(project(":Supervisor"))

    testImplementation(extension.extra["junit-jupiter"].toString())
    testImplementation(extension.extra["scalatest"].toString())
}

sourceSets {
    main {
        scala {
            setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
        }
        java {
            setSrcDirs( listOf<String>() )
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
    test {
        scala {
            setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java"))
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
}
