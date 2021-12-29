/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.0/userguide/multi_project_builds.html
 */

rootProject.name = "ias"
logger.lifecycle("Preparing global settings for {}", rootProject.name)
include(
	"Tools",
	"Cdb",
	"BasicTypes",
	"CdbChecker",
	"KafkaUtils",
	"Heartbeat",
	"CommandsAndReplies",
	"Plugin",
	"PythonPluginFeeder",
	"Converter",
	"CompElement",
	"DistributedUnit",
	"Supervisor",
	"WebServerSender",
	"TransferFunctions",
	"SinkClient",
	"Extras",
	"Monitor")

// Gets the version of python 3 installed in this server
// and stores it in an extra property named PythonVersion
val process = ProcessBuilder("python3", "-V").start()
process.inputStream.reader(Charsets.UTF_8).use {
	val pyVersionStr = it.readText()
	val pyFullVersion = pyVersionStr.drop(pyVersionStr.lastIndexOf(' ')+1)
	val pyVersion = pyFullVersion.dropLast(pyFullVersion.lastIndexOf('.'))
	if (gradle is ExtensionAware) {
		val extension = gradle as ExtensionAware
		extension.extra["PythonVersion"] = pyVersion
	}
}
process.waitFor(10, TimeUnit.SECONDS)

// Sets the version of java in the JdkVersion extra property
// and the comman dependencies
if (gradle is ExtensionAware) {
	val extension = gradle as ExtensionAware
	extension.extra["JdkVersion"] = 11

        // Common dependencies
	extension.extra["scala-library"] = "org.scala-lang:scala3-library_3:3.1.0"
	extension.extra["scalatest"] = "org.scalatest:scalatest_3:3.2.10"
	extension.extra["slf4j-api"] = "org.slf4j:slf4j-api:1.7.32"
	extension.extra["scala-logging"] = "com.typesafe.scala-logging:scala-logging_3:3.9.4"
	extension.extra["logback-classic"] = "ch.qos.logback:logback-classic:1.2.9"
	extension.extra["jackson-databind"] = "com.fasterxml.jackson.core:jackson-databind:2.10.5"
	extension.extra["junit-jupiter-api"] = "org.junit.jupiter:junit-jupiter-api:5.7.2"
	extension.extra["junit-jupiter-engine"] = "org.junit.jupiter:junit-jupiter-engine:5.7.2"
	extension.extra["junit-platform-console-standalone"] = "org.junit.platform:junit-platform-console-standalone:1.7.2"
	extension.extra["commons-cli"] = "commons-cli:commons-cli:1.4"
	extension.extra["kafka-clients"] = "org.apache.kafka:kafka-clients:2.8.0"
	extension.extra["kafka-streams"] = "org.apache.kafka:kafka-streams:2.8.0"
	extension.extra["kafka-connect-api"] = "org.apache.kafka:connect-api:2.8.0"
	extension.extra["hibernate-jpa"] = "org.hibernate.javax.persistence:hibernate-jpa-2.1-api:1.0.0.Final"
}

logger.lifecycle("Global settings for {} ready", rootProject.name)
