plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {

    /* Depend on the kotlin plugin, since we want to access it in our plugin */
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")

    /* Depend on the default Gradle API's since we want to build a custom plugin */
    compileOnly(gradleApi())
    implementation(localGroovy())
    implementation(kotlin("stdlib"))
}

gradlePlugin {
    plugins {
        create("IasPlugin") {
            id = "org.eso.ias.build.plugin"
            implementationClass = "org.eso.ias.build.plugin.IasBuild"
        }
    }
}
