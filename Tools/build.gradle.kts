plugins {
    scala
    java
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    implementation(libs.scala.library)
    implementation(libs.slf4j.api)
    implementation(libs.scala.logging)
    implementation(libs.logback.classic)
    implementation("com.h2database:h2:2.1.214")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.scalatest)
    implementation("org.scalatestplus:junit-5-13_3:3.2.19.0")
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
            setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java")
        }
    }
}

tasks.test {
            useJUnitPlatform()
}

