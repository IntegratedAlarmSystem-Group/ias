plugins {
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.commons.cli)
    implementation(libs.hibernate.jpa)
    implementation(libs.kafka.clients)

    implementation("org.eclipse.jetty.websocket:websocket-api:9.4.15.v20190215")
    implementation("org.eclipse.jetty.websocket:websocket-client:9.4.15.v20190215")
    implementation("org.eclipse.jetty.websocket:websocket-server:9.4.15.v20190215")

    implementation(project(":Cdb"))
    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":KafkaUtils"))
    implementation(project(":Heartbeat"))
    implementation(project(":CommandsAndReplies"))

    testImplementation("org.eclipse.jetty.websocket:websocket-server:9.4.15.v20190215")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}
