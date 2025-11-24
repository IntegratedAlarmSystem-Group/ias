plugins {
    `scala`
    `java`
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
    implementation(extension.extra["jackson-databind"].toString())
    implementation(extension.extra["commons-cli"].toString())
    implementation(extension.extra["hibernate-jpa"].toString())
    implementation(extension.extra["kafka-clients"].toString())
    

    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))
    implementation(project(":KafkaUtils"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":Heartbeat"))

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
    }
    test {
        scala {
            setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java"))
        }
    }
}

tasks.test {
            useJUnitPlatform()
            exclude ("**/HbAlarmGenerationTest.class")
}
