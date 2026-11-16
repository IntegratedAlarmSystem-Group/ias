import java.io.File

plugins {
    base
    scala
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.protobuf.java)
    implementation(libs.javax.annotation)

    implementation(project(":CompElement"))
    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}

sourceSets {
    test {
        scala {
            setSrcDirs(listOf("src/test/scala"))
        }
    }
}

tasks.test {
    dependsOn(project(":PyTfServer").tasks.named("CopyPyMods"))
    val extension = gradle as ExtensionAware
    val pythonVersion = extension.extra["PythonVersion"] as String

    val compElementProject = project(":CompElement")
    val basicTypeProject = project(":BasicTypes")
    val toolsProject = project(":Tools")
    val pyTfServerProject = project(":PyTfServer")

    val buildSitePackages = compElementProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val basicTypesPython  = basicTypeProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val buildToolsPython  = toolsProject.file("build/lib/$pythonVersion/site-packages").absolutePath
    val pyTfServerSrc     = pyTfServerProject.file("src/main/python").absolutePath
    val pyTfServerTest    = pyTfServerProject.file("src/test/python").absolutePath
    val compElementGenProto = compElementProject.file("build/generated/python/IasProtobufTfserver").absolutePath
    val basicTypesGenProto  = basicTypeProject.file("build/generated/python/IasProtobufTypes").absolutePath

    val pythonPath = listOf(
        buildSitePackages, basicTypesPython, buildToolsPython,
        pyTfServerSrc, pyTfServerTest, compElementGenProto, basicTypesGenProto
    ).joinToString(File.pathSeparator)

    environment("PYTHONPATH", pythonPath)
    print("PYTHONPATH $pythonPath")

    useJUnitPlatform()
}

tasks.named<Exec>("pytest").configure {
    dependsOn(project(":CompElement").tasks.named("CopyPyProtoMods"))
}
