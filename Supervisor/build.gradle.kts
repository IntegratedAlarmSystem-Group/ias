plugins {
    id("scala")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")

    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(project(":Cdb"))
    implementation(project(":CommandsAndReplies"))
    implementation(project(":DistributedUnit"))
    implementation(project(":Heartbeat"))
    implementation(project(":CdbChecker"))
    implementation(project(":KafkaUtils"))

    testImplementation("org.scalatest:scalatest_2.13:3.2.5")
    testImplementation(project(":CompElement"))
}

base.archivesBaseName = "iasSupervisor"

repositories {
   mavenCentral()
}


