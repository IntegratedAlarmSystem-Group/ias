plugins {
    base
    id("test-report-aggregation")
}

// Exclude grpc transitive guava that conflicts with test-report-aggregation
configurations.all {
    exclude(group = "com.google.guava", module = "guava")
}


dependencies {
    // Gets the dependencis (i.e. the modules of the project) from aggregatedModules
    // defined in settings.gradle/kts
    val aggregatedModules: List<String> by gradle.extra

    // aggregatedModules.forEach { moduleName ->
        // testReportAggregation(project(":$moduleName"))
        
    // aggregatedModules.forEach { moduleName ->
    //     val projectRef = project(":$moduleName")
    //     if (projectRef.pluginManager.hasPlugin("java")) {
    //         testReportAggregation(projectRef)
    //     }
    // }
    
    aggregatedModules.forEach { moduleName ->
        val otherProject = findProject(":$moduleName")
        if (
            otherProject != null &&
            (otherProject.pluginManager.hasPlugin("java") || otherProject.pluginManager.hasPlugin("scala") || otherProject.pluginManager.hasPlugin("pytest"))) {
            testReportAggregation(otherProject)
        }
    }
}


reporting {
    reports {
        val testAggregateTestReport by creating(AggregateTestReport::class) { // <.>
            testSuiteName = "test"
        }
    }
}

tasks.check {
    dependsOn(tasks.named<TestReport>("testAggregateTestReport")) // <.>
}

