plugins {
    java
    scala
    id("io.gatling.gradle") version "3.11.5.2"
}

// Set Java compatibility - Scala 2.13 works best with Java 21 LTS
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Configure Scala compiler to target JVM 21
tasks.withType<ScalaCompile> {
    scalaCompileOptions.additionalParameters = listOf("-release:21")
}

repositories {
    mavenCentral()
}

dependencies {
    // Gatling dependencies
    gatling("io.gatling.highcharts:gatling-charts-highcharts:3.11.5")
    gatling("io.gatling:gatling-app:3.11.5")
    
    // JSON handling
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-scala_2.13:2.17.0")
}

gatling {
    // JVM settings for Gatling simulations
    jvmArgs = listOf(
        "-Xms512m",
        "-Xmx1g",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        "-Dgatling.core.outputDirectoryBaseName=gatling"
    )
}

// Task to run specific simulations
tasks.register("loadTestConcurrentLogins") {
    group = "performance"
    description = "Run PERF-LOAD-001: 100 Concurrent User Logins"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.load.ConcurrentLoginsSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("loadTestConcurrentBookings") {
    group = "performance"
    description = "Run PERF-LOAD-002: 50 Concurrent Appointment Bookings"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.load.ConcurrentBookingsSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("loadTestConcurrentQueries") {
    group = "performance"
    description = "Run PERF-LOAD-003: 1000 Concurrent Read Queries"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.load.ConcurrentQueriesSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("loadTestGatewayRouting") {
    group = "performance"
    description = "Run PERF-LOAD-004: Gateway Routing Under Load"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.load.GatewayRoutingSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("combinedBusinessFlow") {
    group = "performance"
    description = "Run PERF-LOAD-001 (500 VU): Combined Business Flow"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.load.CombinedBusinessFlowSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("stressTestDbPool") {
    group = "performance"
    description = "Run PERF-STRESS-001: Database Connection Pool Limits"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.stress.DbConnectionPoolSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("stressTestMemory") {
    group = "performance"
    description = "Run PERF-STRESS-002: Memory Usage Under Load"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.stress.MemoryUsageSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("stressTestRecovery") {
    group = "performance"
    description = "Run PERF-STRESS-003: Service Recovery After Failure"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.stress.ServiceRecoverySimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("enduranceTest24h") {
    group = "performance"
    description = "Run PERF-END-001: 24-Hour Continuous Operation"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.endurance.ContinuousOperationSimulation")
    }
    finalizedBy("gatlingRun")
}

tasks.register("enduranceTestMemoryLeak") {
    group = "performance"
    description = "Run PERF-END-002: Memory Leak Detection"
    doFirst {
        System.setProperty("gatling.simulation", "simulations.endurance.MemoryLeakDetectionSimulation")
    }
    finalizedBy("gatlingRun")
}

// Task to run all load tests
tasks.register("runAllLoadTests") {
    group = "performance"
    description = "Run all load tests"
    dependsOn(
        "loadTestConcurrentLogins",
        "loadTestConcurrentBookings",
        "loadTestConcurrentQueries",
        "loadTestGatewayRouting"
    )
}
