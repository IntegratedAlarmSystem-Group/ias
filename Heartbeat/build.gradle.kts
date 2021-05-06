plugins {
    `scala`
    `java`
    `java-library-distribution`
}

dependencies {
    // Scala
    implementation("org.scala-lang:scala-library:2.13.5")
    
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.3")
//    implementation("ch.qos.logback:logback-classic:1.2.3")
//    implementation("com.fasterxml.jackson.core:jackson-core:2.9.4")
//    implementation("com.fasterxml.jackson.core:jackson-annotations:2.9.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.4")

    testImplementation("org.scalactic:scalactic_2.13:3.2.7")
    testImplementation("org.scalatest:scalatest_2.13:3.2.7")

    implementation(project(":KafkaUtils"))
    implementation(project(":Tools"))

//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
}

base.archivesBaseName = "iasHeartbeat"

//tasks.withType<ScalaCompile>{
//    // Groovy only needs the declared dependencies
//    // (and not longer the output of compileJava)
//    classpath = sourceSets.main.get().compileClasspath
//}

//tasks.withType<JavaCompile> {
//    this.dependsOn(tasks.compileScala)
//    val compilerArgs = options.compilerArgs
//    compilerArgs.add("-Xdoclint:all,-missing")
//    compilerArgs.add("-Xlint:all")
//    classpath += files(sourceSets.main.get().withConvention(ScalaSourceSet::class) { scala }.classesDirectory)
//}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/main/scala")+listOf("src/main/java"))
                //setSrcDirs(listOf("src/main/java"))
            }
            java {
                //setSrcDirs(listOf("src/main/java"))
                setSrcDirs( listOf<String>() )
            }
        }
    }
    test {
        withConvention(ScalaSourceSet::class) {
            scala {
                setSrcDirs(listOf("src/test/scala")+listOf("src/test/java"))
            }
        }
    }
}


