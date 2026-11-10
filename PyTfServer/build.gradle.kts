plugins {
    base
    id("org.eso.ias.build.plugin")
}

tasks.named<Exec>("pytest").configure {
    dependsOn(project(":CompElement").tasks.named("CopyPyProtoMods"))
}
