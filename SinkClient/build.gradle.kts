plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.logback.classic)
    implementation(libs.kafka.clients)
    implementation(libs.commons.cli)
    implementation(libs.kafka.connect.api)
    
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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
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
            setSrcDirs(listOf("src/test/scala"))
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
