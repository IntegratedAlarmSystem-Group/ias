import java.io.File

plugins {
    scala
    java
    `java-library-distribution`
    id("com.google.protobuf") version "0.9.4"
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.logback.classic)

    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.protobuf.java)
    implementation(libs.javax.annotation)
    constraints {
        implementation("com.google.guava:guava:33.2.1-jre") {
            because("grpc needs jre variant, not android")
        }
    }

    implementation(project(":Tools"))
    implementation(project(":BasicTypes"))
    implementation(project(":Cdb"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}

sourceSets {
    main {
        scala {
            setSrcDirs(listOf("src/main/scala", "src/main/java"))
        }
        java {
            // Main Java sources are compiled by Scala compiler;
            // only keep generated proto sources for compileJava
            setSrcDirs(listOf("build/generated/source/proto/main/java",
                              "build/generated/source/proto/main/grpc"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("src/test/java"))
        }
        scala {
            setSrcDirs(listOf("src/test/scala"))
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.generateProto)
}

tasks.withType<ScalaCompile> {
    dependsOn(tasks.generateProto)
    dependsOn(tasks.compileJava)
    classpath += tasks.compileJava.get().destinationDirectory.asFileTree
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        register("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                register("grpc")
            }
        }
    }
}

// Generate Python proto stubs into IasProtobufTfserver package
tasks.register<Exec>("generatePythonProto") {
    dependsOn(tasks.generateProto)
    val pythonProtoOut = file("build/generated/python/IasProtobufTfserver")
    val extractedProtos = file("build/extracted-protos/main")
    val protoSource = file("src/main/proto")
    val protoFiles = protoSource.listFiles { f -> f.extension == "proto" }?.toList() ?: emptyList()
    inputs.dir(protoSource)
    outputs.dir(pythonProtoOut)
    executable = "python3"
    args = listOf(
        "-m", "grpc_tools.protoc",
        "--proto_path=${protoSource.absolutePath}",
        "--proto_path=${extractedProtos.absolutePath}",
        "--python_out=${pythonProtoOut.absolutePath}",
        "--grpc_python_out=${pythonProtoOut.absolutePath}"
    ) + protoFiles.map { it.name }
}

// Copy generated Python proto stubs to site-packages so InstallLib picks them up
val pyExtension = gradle as ExtensionAware
val pyVersion = pyExtension.extra["PythonVersion"] as String

tasks.register<Copy>("CopyPyProtoMods") {
    dependsOn("generatePythonProto")
    from(project.layout.projectDirectory.dir("build/generated/python/IasProtobufTfserver")) {
        include("*.py")
    }
    from(project.layout.projectDirectory.dir("src/main/python/IasProtobufTfserver")) {
        include("__init__.py")
    }
    into(project.layout.buildDirectory.dir("lib/${pyVersion}/site-packages/IasProtobufTfserver"))
}

tasks.named("processResources") {
    dependsOn("CopyPyProtoMods")
}

tasks.test {
    val extension = gradle as ExtensionAware
    val pythonVersion = extension.extra["PythonVersion"] as String

    val pythonRoot = System.getenv("Python_ROOT_DIR")

    val compElementProject = project(":CompElement")
    val basicTypeProject = project(":BasicTypes")
    val toolsProject = project(":Tools")

    val buildSitePackages =  compElementProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val builBasicTypesPython = basicTypeProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val buildToolsPython = toolsProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val srcTestPython      = compElementProject.file("src/test/python").absolutePath

    val pythonPath = listOf(buildSitePackages, builBasicTypesPython, buildToolsPython, srcTestPython)
        .joinToString(File.pathSeparator)

    environment("PYTHONPATH", pythonPath)
    print("PYTHONPATH $pythonPath")

    exclude("org/eso/ias/asce/test/TestPyTF*.class")
    exclude("org/eso/ias/asce/test/TestMinMaxPyTF*.class")

    useJUnitPlatform()

    doFirst {
        println(">>> Python_ROOT_DIR=$pythonRoot")
        println(">>> PYTHONPATH=$pythonPath")
        println(">>> JAVA_HOME=${System.getenv("JAVA_HOME")}")
        println(">>> LD_LIBRARY_PATH=${System.getenv("LD_LIBRARY_PATH")}")
        println(">>> java.library.path=${System.getProperty("java.library.path")}")
    }
}
