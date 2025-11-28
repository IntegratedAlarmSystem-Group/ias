plugins {
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    val g = project.gradle
    val extension = g as ExtensionAware
    implementation(extension.extra["commons-cli"].toString())
    implementation(extension.extra["jackson-databind"].toString())
    implementation(extension.extra["slf4j-api"].toString())
    implementation(extension.extra["hibernate-jpa"].toString())
    
    implementation(project(":Plugin"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":CommandsAndReplies"))

    testImplementation(extension.extra["junit-jupiter"].toString())
}

sourceSets {
    test {
        resources {
            setSrcDirs(listOf("src/test/resources"))
        }
    }
}

tasks.test {
            useJUnitPlatform()
            exclude("**/UdpPluginTest.class")
}
