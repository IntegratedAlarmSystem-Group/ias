plugins {
    `scala`
    `java`
    `java-library-distribution`
    id("org.eso.ias.build.plugin")
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.5")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.5")

    testImplementation("org.scalactic:scalactic_2.13:3.2.7")
    implementation("org.scalatest:scalatest_2.13:3.2.9")

    implementation(project(":Tools"))
    implementation(project(":Cdb"))

    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    implementation("org.junit.platform:junit-platform-console:1.7.2")
}

base.archivesBaseName = "ias"+project.name
base.libsDirName ="lib"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    main {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
            }
            java {
                setSrcDirs( listOf<String>() )
            }
        }
    }
    test {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/test/scala")) // +listOf("src/test/java"))
            }
        }
    }
}


