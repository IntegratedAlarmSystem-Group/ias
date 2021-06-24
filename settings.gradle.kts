/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.0/userguide/multi_project_builds.html
 */

rootProject.name = "ias"
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
	extension.extra["scala-library"] = "org.scala-lang:scala-library:2.13.5"
	extension.extra["scalatest"] = "org.scalatest:scalatest_2.13:3.2.9"
	extension.extra["slf4j-api"] = "org.slf4j:slf4j-api:1.7.30"
	extension.extra["scala-logging"] = "com.typesafe.scala-logging:scala-logging_2.13:3.9.3"
	extension.extra["logback-classic"] = "ch.qos.logback:logback-classic:1.2.3"
	extension.extra["jackson-databind"] = "com.fasterxml.jackson.core:jackson-databind:2.10.5"
	extension.extra["junit-jupiter-api"] = "org.junit.jupiter:junit-jupiter-api:5.7.2"
	extension.extra["junit-jupiter-engine"] = "org.junit.jupiter:junit-jupiter-engine:5.7.2"
}

