package simulations.stress

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.util.UUID

/**
 * PERF-STRESS-001: Database Connection Pool Limits
 * 
 * Push database connection pool to its limits and test graceful degradation/recovery.
 *
 * Test Configuration:
 * - Max connections: 100
 * - Min idle: 10
 * - Connection timeout: 30 seconds
 *
 * Test Steps:
 * 1. Gradually increase concurrent database operations:
 *    - Start: 50 concurrent users
 *    - Increment: +25 users every minute
 *    - Peak: 300 concurrent users (3x pool size)
 * 2. Each user performs complex queries:
 *    - JOIN operations across 3-4 tables
 *    - Transaction with multiple INSERT/UPDATE
 *    - Hold connection for 2-5 seconds
 * 3. Monitor until connection pool exhaustion
 * 4. Reduce load and verify recovery
 *
 * Acceptance Criteria:
 * - Connection pool reaches 100 active connections
 * - Additional requests queue (not fail immediately)
 * - Connection timeout after 30 seconds for waiting requests
 * - Proper error handling: 503 Service Unavailable
 * - No connection leaks after load reduction
 * - Pool recovers to baseline within 60 seconds
 * - Circuit breaker opens to protect database
 * - Zero data corruption or transaction failures
 */
class DbConnectionPoolSimulation extends HmsSimulationBase {

  // Feeders for authenticated users performing database operations
  val staffFeeder = {
    val doctors = (1 to doctorCount).map(i => Map(
      "email" -> s"doctor$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "DOCTOR",
      "staffIndex" -> i
    ))
    val nurses = (1 to nurseCount).map(i => Map(
      "email" -> s"nurse$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "NURSE",
      "staffIndex" -> i
    ))
    val receptionists = (1 to receptionistCount).map(i => Map(
      "email" -> s"receptionist$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "RECEPTIONIST",
      "staffIndex" -> i
    ))
    scala.util.Random.shuffle(doctors ++ nurses ++ receptionists).toArray.circular
  }

  // Patient feeder for write operations
  val patientWriteFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"patient${(i % patientCount) + 1}@email.com",
    "password" -> defaultPassword,
    "patientIndex" -> ((i % patientCount) + 1)
  ))

  /**
   * Complex query scenario that holds database connections longer.
   * Simulates JOIN operations across multiple tables.
   */
  val complexQueryScenario = scenario("Complex DB Queries (Connection Pool Stress)")
    .feed(staffFeeder)
    // Login to get auth token
    .exec(
      http("Login")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(500.milliseconds)
    // Continuous complex operations to exhaust connection pool
    .forever {
      // Complex Query 1: Patient list with pagination (holds connection)
      exec(
        http("GET /patients (Paginated - Complex Query)")
          .get("/patients")
          .queryParam("page", "${staffIndex}")
          .queryParam("size", "50")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 503, 504, 429))
          .check(responseTimeInMillis.saveAs("queryTime"))
      )
      .pause(1.second, 2.seconds)
      
      // Complex Query 2: Appointments with doctor JOIN
      .exec(
        http("GET /appointments (Doctor Schedule - JOIN Query)")
          .get("/appointments/by-doctor/emp-doctor-${staffIndex}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 503, 504, 429))
      )
      .pause(1.second, 2.seconds)
      
      // Complex Query 3: Medical exams with patient and prescription JOINs
      .exec(
        http("GET /exams (Patient History - Multi-JOIN)")
          .get("/exams")
          .queryParam("page", "0")
          .queryParam("size", "100")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 503, 504, 429))
      )
      .pause(2.seconds, 5.seconds) // Longer pause to simulate real usage
    }

  /**
   * Transactional write scenario that creates multiple related records.
   * Tests transaction handling under connection pool pressure.
   */
  val transactionalWriteScenario = scenario("Transactional Writes (Connection Pool Stress)")
    .feed(patientWriteFeeder)
    // Login as patient
    .exec(
      http("Login")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
        .check(jsonPath("$.data.user.id").optional.saveAs("patientId"))
    )
    .pause(500.milliseconds)
    // Continuous transactional operations
    .forever {
      // Transaction 1: Create appointment (INSERT with foreign key checks)
      exec(session => {
        val uniqueId = UUID.randomUUID().toString.take(8)
        val doctorIndex = scala.util.Random.nextInt(doctorCount) + 1
        session
          .set("uniqueId", uniqueId)
          .set("selectedDoctorId", s"emp-doctor-$doctorIndex")
      })
      .exec(
        http("POST /appointments (Transactional Write)")
          .post("/appointments")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(
            """{
              "patientId": "${patientId}",
              "doctorId": "${selectedDoctorId}",
              "type": "CONSULTATION",
              "reason": "Connection Pool Stress Test - ${uniqueId}",
              "notes": "Testing transaction under load"
            }"""
          )).asJson
          .check(status.in(200, 201, 400, 409, 503, 504, 429))
          .check(jsonPath("$.data.id").optional.saveAs("appointmentId"))
      )
      .pause(2.seconds, 4.seconds)
      
      // Transaction 2: Update patient record (UPDATE with validation)
      .exec(
        http("PUT /patients (Update - Transaction)")
          .put("/patients/${patientId}")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(
            """{
              "notes": "Updated at ${uniqueId} - Pool Stress Test"
            }"""
          )).asJson
          .check(status.in(200, 400, 404, 503, 504, 429))
      )
      .pause(2.seconds, 5.seconds)
    }

  /**
   * Long-running query scenario that holds connections for extended periods.
   * Simulates report generation and bulk data exports.
   */
  val longRunningQueryScenario = scenario("Long-Running Queries (Connection Holding)")
    .feed(staffFeeder)
    .exec(
      http("Login")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
    .forever {
      // Large dataset query - patients with full details
      exec(
        http("GET /patients (Large Dataset - 200 records)")
          .get("/patients")
          .queryParam("page", "0")
          .queryParam("size", "200")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 503, 504, 429))
      )
      .pause(3.seconds, 5.seconds) // Simulate processing time
      
      // Bulk appointments query
      .exec(
        http("GET /appointments (Bulk Query)")
          .get("/appointments")
          .queryParam("page", "0")
          .queryParam("size", "200")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 503, 504, 429))
      )
      .pause(3.seconds, 5.seconds)
      
      // Cross-service query (triggers Feign calls)
      .exec(
        http("GET /schedules/doctors (Cross-Service Query)")
          .get("/schedules/doctors")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 503, 504, 429))
      )
      .pause(5.seconds, 10.seconds) // Extended pause for connection holding
    }

  /**
   * Recovery monitoring scenario - runs during ramp-down to verify pool recovery.
   */
  val recoveryMonitorScenario = scenario("Pool Recovery Monitor")
    .feed(staffFeeder)
    .exec(
      http("Login")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .repeat(10) {
      exec(
        http("GET /patients (Recovery Check)")
          .get("/patients")
          .queryParam("page", "0")
          .queryParam("size", "10")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200)) // Should succeed during recovery
          .check(responseTimeInMillis.lte(5000)) // Should respond within 5s during recovery
      )
      .pause(5.seconds)
    }

  setUp(
    // Phase 1: Initial load (50 users) - establish baseline
    complexQueryScenario.inject(
      rampUsers(50).during(30.seconds),
      nothingFor(1.minute)
    ),
    
    // Phase 2: Increase to 100 users (pool capacity)
    transactionalWriteScenario.inject(
      nothingFor(90.seconds),
      rampUsers(50).during(30.seconds),
      nothingFor(1.minute)
    ),
    
    // Phase 3: Increase to 200 users (2x pool capacity - stress)
    longRunningQueryScenario.inject(
      nothingFor(3.minutes),
      rampUsers(100).during(1.minute),
      nothingFor(2.minutes)
    ),
    
    // Phase 4: Peak at 300 users (3x pool capacity - severe stress)
    complexQueryScenario.inject(
      nothingFor(6.minutes),
      rampUsers(100).during(1.minute),
      nothingFor(2.minutes)
    ),
    
    // Phase 5: Recovery monitoring (after load reduction)
    recoveryMonitorScenario.inject(
      nothingFor(9.minutes),
      atOnceUsers(10)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Connection pool should handle baseline load without errors
     global.failedRequests.percent.lt(30.0), // Allow higher error rate during extreme stress
     
     // During recovery phase, success rate should be high
     details("GET /patients (Recovery Check)").successfulRequests.percent.gte(90.0),
     
     // Baseline queries should mostly succeed
     details("GET /patients (Paginated - Complex Query)").responseTime.percentile(95).lt(10000),
     
     // System should not crash completely
     global.responseTime.percentile(99).lt(35000) // 30s timeout + buffer
   )
   .maxDuration(12.minutes) // Extended duration for stress + recovery
}
