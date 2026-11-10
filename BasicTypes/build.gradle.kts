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
    implementation(libs.jackson.databind)

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
            setSrcDirs(listOf("build/generated/source/proto/main/java",
                              "build/generated/source/proto/main/grpc"))
        }
    }
    test {
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

tasks.register<Exec>("generatePythonProto") {
    val pythonProtoOut = file("build/generated/python/IasProtobufTypes")
    val extractedProtos = file("build/extracted-protos/main")
    val protoSource = file("src/main/proto")
    val protoFiles = protoSource.walk().filter { it.isFile && it.extension == "proto" }.toList()
    inputs.dir(protoSource)
    outputs.dir(pythonProtoOut)
    executable = "python3"
    args = listOf(
        "-m", "grpc_tools.protoc",
        "--proto_path=${protoSource.absolutePath}",
        "--proto_path=${extractedProtos.absolutePath}",
        "--python_out=${pythonProtoOut.absolutePath}",
        "--grpc_python_out=${pythonProtoOut.absolutePath}"
    ) + protoFiles.map { f -> f.relativeTo(protoSource).toString() }
}

// Fix flat imports in generated Python proto stubs.
// BasicTypes has no cross-module imports currently, but the fix is wired for future proofing.
val fixGrpcImportsScript = project(":CompElement").file("src/main/python/fix_grpc_imports")

tasks.register<Exec>("fixPythonGrpcImports") {
    description = "Fix flat imports in BasicTypes generated proto stubs"
    dependsOn("generatePythonProto")
    mustRunAfter("generatePythonProto")
    val pythonProtoOut = file("build/generated/python/IasProtobufTypes")
    executable = "python3"
    args = listOf(
        fixGrpcImportsScript.absolutePath,
        pythonProtoOut.absolutePath,
        "IasProtobufTypes",
        "-g", "*_pb2_grpc.py"
    )
}

val pyExtension = gradle as ExtensionAware
val pyVersion = pyExtension.extra["PythonVersion"] as String

tasks.register<Copy>("CopyPyProtoMods") {
    dependsOn("fixPythonGrpcImports")
    from(project.layout.projectDirectory.dir("build/generated/python/IasProtobufTypes")) {
        include("*.py")
    }
    from(project.layout.projectDirectory.dir("src/main/python/IasProtobufTypes")) {
        include("__init__.py")
    }
    into(project.layout.buildDirectory.dir("lib/${pyVersion}/site-packages/IasProtobufTypes"))
}

tasks.named("processResources") {
    dependsOn("CopyPyProtoMods")
}

tasks.test {
            useJUnitPlatform()
}

tasks.named("pytest").configure {
    dependsOn("CopyPyProtoMods")
    dependsOn(project(":Tools").tasks.named("build"))
}
