plugins {
    base
    id("org.eso.ias.build.plugin")

}

abstract class GuiTask : DefaultTask() {
   @TaskAction
    fun build() {
        println("Building GUI...")
    } 
}

tasks.register<GuiTask>("Gui") {
    dependsOn(":build")
}

tasks.named("pytest").configure {
    dependsOn(project(":Cdb").tasks.named("build"))
    dependsOn(project(":BasicTypes").tasks.named("build"))
    dependsOn(project(":KafkaUtils").tasks.named("build"))
    dependsOn(project(":Tools").tasks.named("build"))
}
