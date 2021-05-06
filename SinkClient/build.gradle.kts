plugins {
    `scala`
    `java`
    `java-library-distribution`
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.apache.kafka:kafka-clients:2.8.0")
    implementation("com.datastax.cassandra:cassandra-driver-core:3.6.0")
    implementation("org.apache.kafka:connect-api:2.8.0")
    implementation("javax.mail:mail:1.4.7")
    implementation("commons-cli:commons-cli:1.4")

    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":DistributedUnit"))
    implementation(project(":Supervisor"))

    testImplementation("org.scalatest:scalatest_2.13:3.2.5")
}

base.archivesBaseName = "iasSinkClient"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
                //setSrcDirs(listOf("src/main/java"))
            }
            java {
                //setSrcDirs(listOf("src/main/java"))
                setSrcDirs( listOf<String>() )
            }
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
    test {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/test/scala")+listOf("src/test/java"))
            }
        }
    }
}


