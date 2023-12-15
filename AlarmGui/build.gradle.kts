plugins {
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
