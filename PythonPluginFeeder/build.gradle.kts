plugins {
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.commons.cli)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.hibernate.jpa)
    implementation(libs.logback.classic)

    implementation(project(":Plugin"))
    implementation(project(":Heartbeat"))
    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":CommandsAndReplies"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

sourceSets {
    test {
        resources {
            setSrcDirs(listOf("src/test/resources"))
        }
    }
}
