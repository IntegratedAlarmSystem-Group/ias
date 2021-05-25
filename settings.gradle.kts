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

println("||| Settings")
val process = ProcessBuilder("python3", "-V").start()
process.inputStream.reader(Charsets.UTF_8).use {

	val pyVersionStr = it.readText()
	println("out ===> $pyVersionStr")
	val pyFullVersion = pyVersionStr.drop(pyVersionStr.lastIndexOf(' ')+1)
	val pyVersion = pyFullVersion.dropLast(pyFullVersion.lastIndexOf('.'))
	if (gradle is ExtensionAware) {
		val extension = gradle as ExtensionAware
		extension.extra["PythonVersion"] = pyVersion
		val temp = extension.extra["PythonVersion"]
		println("temp ===> $temp")
	}
}
process.waitFor(10, TimeUnit.SECONDS)

println("||| Settings done")