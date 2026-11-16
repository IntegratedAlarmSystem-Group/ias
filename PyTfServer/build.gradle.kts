plugins {
    `java-library-distribution`
    scala
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(project(":CompElement"))
    implementation(project(":BasicTypes"))
    implementation(project(":Tools"))
    implementation(libs.logback.classic)
    implementation(libs.scala.library)

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

    // Register Scala sources for integration tests (root build.gradle.kts only
    // wires java + resources, but our integration tests are Scala)
    named("integrationTest") {
        scala {
            srcDir("src/integrationTest/scala")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
