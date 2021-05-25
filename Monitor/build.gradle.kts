plugins {
    `scala`
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")
//    implementation("ch.qos.logback:logback-classic:1.2.3")
//    implementation("com.fasterxml.jackson.core:jackson-core:2.9.4")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.9.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")
//
//    testImplementation("org.scalactic:scalactic_2.13:3.2.7")
//    testImplementation("org.scalatest:scalatest_2.13:3.2.7")

    implementation("commons-cli:commons-cli:1.4")
    implementation("org.hibernate.javax.persistence:hibernate-jpa-2.1-api:1.0.0.Final")
//
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":Heartbeat"))

    testImplementation("org.scalatest:scalatest_2.13:3.2.5")
    testRuntimeOnly("org.scalactic:scalactic_2.13:3.2.7")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
            }
            java {
                setSrcDirs( listOf<String>() )
            }
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


