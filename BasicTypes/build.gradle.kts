plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.scala.logging)
    implementation(libs.logback.classic)
    implementation(libs.jackson.databind)
    
    implementation(project(":Tools"))
    implementation(project(":Cdb"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
}

sourceSets {
    main {
        scala {
            setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
        }
        java {
            setSrcDirs( listOf<String>() )
        }
    }
    test {
        scala {
            setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java"))
        }
    }
}

tasks.test {
            useJUnitPlatform()
}

tasks.named("pytest").configure {
    dependsOn(project(":Tools").tasks.named("build"))
}

