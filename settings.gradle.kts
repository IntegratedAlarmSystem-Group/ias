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
	"Monitor",
	"AlarmGui")

// Gets the version of python 3 installed in this server
// and stores it in an extra property named PythonVersion
val process = ProcessBuilder("python3", "-V").start()
process.inputStream.reader(Charsets.UTF_8).use {
	val pyVersionStr = it.readText()
	val pyFullVersion = pyVersionStr.drop(pyVersionStr.lastIndexOf(' ')+1)

	var pyVersion = ""
	if (pyVersionStr.count{it== '.'}==2) {
		val idx = pyFullVersion.lastIndexOf('.')
		pyVersion = pyFullVersion.substring(0,idx)
	} else {
		pyVersion = pyFullVersion
	}
	if (gradle is ExtensionAware) {
		val extension = gradle as ExtensionAware
		extension.extra["PythonVersion"] = pyVersion
	}
}
process.waitFor(10, TimeUnit.SECONDS)

val gitBranchProc = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD").start()
gitBranchProc.inputStream.reader(Charsets.UTF_8).use {
	val gitBranch = it.readText()
	if (gradle is ExtensionAware) {
		val extension = gradle as ExtensionAware
		extension.extra["GitBranch"] = gitBranch
	}
}
gitBranchProc.waitFor(10, TimeUnit.SECONDS)

// Sets common dependencies
if (gradle is ExtensionAware) {
	val extension = gradle as ExtensionAware

        // IAS common dependencies
	extension.extra["scala-library"] = "org.scala-lang:scala3-library_3:3.5.1"
	extension.extra["scalatest"] = "org.scalatest:scalatest_3:3.2.17"
	extension.extra["slf4j-api"] = "org.slf4j:slf4j-api:2.0.9"
	extension.extra["scala-logging"] = "com.typesafe.scala-logging:scala-logging_3:3.9.5"
	extension.extra["logback-classic"] = "ch.qos.logback:logback-classic:1.4.11"
	extension.extra["jackson-databind"] = "com.fasterxml.jackson.core:jackson-databind:2.17.0"
	extension.extra["junit-jupiter-api"] = "org.junit.jupiter:junit-jupiter-api:5.10.0"
	extension.extra["junit-jupiter-engine"] = "org.junit.jupiter:junit-jupiter-engine:5.10.0"
	extension.extra["junit-platform-console-standalone"] = "org.junit.platform:junit-platform-console-standalone:1.10.0"
	extension.extra["commons-cli"] = "commons-cli:commons-cli:1.5.0"
	extension.extra["kafka-clients"] = "org.apache.kafka:kafka-clients:3.7.0"
	extension.extra["kafka-streams"] = "org.apache.kafka:kafka-streams:3.7.0"
	extension.extra["kafka-connect-api"] = "org.apache.kafka:connect-api:3.7.0"
	extension.extra["hibernate-jpa"] = "org.hibernate.javax.persistence:hibernate-jpa-2.1-api:1.0.0.Final"
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
}

logger.lifecycle("Global settings for {} ready", rootProject.name)
