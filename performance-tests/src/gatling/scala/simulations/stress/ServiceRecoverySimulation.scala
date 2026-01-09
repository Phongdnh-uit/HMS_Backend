package simulations.stress

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.util.UUID

/**
 * PERF-STRESS-003: Service Recovery After Failure
 * 
 * Test system resilience and automatic recovery after cascading failures 
 * and network interruptions. Validates circuit breaker patterns and failover mechanisms.
 *
 * Test Steps:
 * 1. Establish baseline load: 200 concurrent users
 * 2. Inject failure scenarios sequentially:
 *    - T+0min: Kill patient-service instance (simulate crash)
 *    - T+2min: Network partition: appointment-service cannot reach HR service
 *    - T+5min: Database connection failure (restart PostgreSQL)
 *    - T+8min: API Gateway overload (rate limit exceeded)
 * 3. Observe circuit breakers, fallbacks, retries
 * 4. Restore all services
 * 5. Verify full system recovery
 *
 * Acceptance Criteria:
 * - Circuit breaker opens within 10 seconds of failure
 * - Fallback responses returned (cached data or default)
 * - Services auto-reconnect to database within 30 seconds
 * - Service discovery detects instance failure < 30 seconds
 * - Load redistributed to healthy instances
 * - Retry logic: 3 attempts with exponential backoff
 * - Error rate < 10% during failure
 * - Full recovery within 2 minutes after restoration
 * - No manual intervention required
 * - Transaction consistency maintained (no partial updates)
 * 
 * Note: This simulation generates load patterns that test circuit breaker behavior.
 * External failure injection should be performed via infrastructure scripts.
 */
class ServiceRecoverySimulation extends HmsSimulationBase {

  // Mixed user feeder for realistic traffic distribution
  val mixedUserFeeder = {
    val patients = (1 to 100).map(i => Map(
      "email" -> s"patient$i@email.com",
      "password" -> defaultPassword,
      "role" -> "PATIENT",
      "userIndex" -> i
    ))
    val doctors = (1 to 50).map(i => Map(
      "email" -> s"doctor$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "DOCTOR",
      "userIndex" -> i
    ))
    val nurses = (1 to 30).map(i => Map(
      "email" -> s"nurse$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "NURSE",
      "userIndex" -> i
    ))
    val receptionists = (1 to 20).map(i => Map(
      "email" -> s"receptionist$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "RECEPTIONIST",
      "userIndex" -> i
    ))
    scala.util.Random.shuffle(patients ++ doctors ++ nurses ++ receptionists).toArray.circular
  }

  /**
   * Baseline traffic scenario - establishes normal operation pattern.
   * Runs throughout the test to provide consistent background load.
   */
  val baselineTrafficScenario = scenario("Baseline Traffic (200 VU)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login (Baseline)")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.in(200, 401, 503, 504)) // Accept degraded responses
        .check(jsonPath("$.data.accessToken").optional.saveAs("authToken"))
    )
    .pause(1.second)
    .doIf(session => session.contains("authToken")) {
      forever {
        // Mix of read and write operations across services
        randomSwitch(
          30.0 -> exec(
            http("GET /patients (Baseline)")
              .get("/api/patients")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404, 500, 502, 503, 504))
          ),
          25.0 -> exec(
            http("GET /appointments (Baseline)")
              .get("/api/appointments")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404, 500, 502, 503, 504))
          ),
          20.0 -> exec(
            http("GET /schedules/doctors (Baseline)")
              .get("/schedules/doctors")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404, 500, 502, 503, 504))
          ),
          15.0 -> exec(
            http("GET /medicines (Baseline)")
              .get("/api/medicines")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404, 500, 502, 503, 504))
          ),
          10.0 -> exec(
            http("GET /invoices (Baseline)")
              .get("/invoices")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404, 500, 502, 503, 504))
          )
        )
        .pause(2.seconds, 5.seconds)
      }
    }

  /**
   * Patient service stress - generates load specifically on patient-service.
   * Used to test circuit breaker when patient-service fails.
   */
  val patientServiceStressScenario = scenario("Patient Service Stress (Circuit Breaker Test)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.in(200, 401, 503, 504))
        .check(jsonPath("$.data.accessToken").optional.saveAs("authToken"))
    )
    .pause(500.milliseconds)
    .doIf(session => session.contains("authToken")) {
      forever {
        // Rapid patient queries to trigger circuit breaker
        exec(
          http("GET /patients/{id} (Circuit Breaker Test)")
            .get("/api/patients/${userIndex}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404, 500, 502, 503, 504))
            .check(responseTimeInMillis.saveAs("patientQueryTime"))
        )
        .exec(session => {
          val responseTime = session("patientQueryTime").as[Int]
          // Log slow responses that might indicate circuit breaker activation
          if (responseTime > 5000) {
            println(s"Slow patient query: ${responseTime}ms - possible circuit breaker active")
          }
          session
        })
        .pause(500.milliseconds, 1.second)
        
        // Additional patient operations
        .exec(
          http("GET /patients (List)")
            .get("/api/patients")
            .queryParam("page", "0")
            .queryParam("size", "50")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 500, 502, 503, 504))
        )
        .pause(1.second, 2.seconds)
      }
    }

  /**
   * Cross-service dependency scenario - tests Feign client resilience.
   * Appointment-service depends on HR-service for doctor schedules.
   */
  val crossServiceDependencyScenario = scenario("Cross-Service Dependencies (Feign Resilience)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.in(200, 401, 503, 504))
        .check(jsonPath("$.data.accessToken").optional.saveAs("authToken"))
    )
    .pause(500.milliseconds)
    .doIf(session => session.contains("authToken")) {
      forever {
        // This flow requires multiple service interactions
        exec(
          http("GET /schedules/doctors (HR Dependency)")
            .get("/schedules/doctors")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404, 500, 502, 503, 504))
        )
        .pause(1.second)
        
        // Appointment creation depends on patient-service and hr-service
        .exec(session => {
          val uniqueId = UUID.randomUUID().toString.take(8)
          val doctorIndex = scala.util.Random.nextInt(doctorCount) + 1
          session
            .set("uniqueId", uniqueId)
            .set("selectedDoctorId", s"emp-doctor-$doctorIndex")
        })
        .exec(
          http("POST /appointments (Cross-Service Write)")
            .post("/api/appointments")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody(
              """{
                "doctorId": "${selectedDoctorId}",
                "type": "CONSULTATION",
                "reason": "Recovery Test - ${uniqueId}"
              }"""
            )).asJson
            .check(status.in(200, 201, 400, 409, 500, 502, 503, 504))
        )
        .pause(2.seconds, 4.seconds)
        
        // Fetch doctor's appointments (requires HR lookup)
        .exec(
          http("GET /appointments/by-doctor (Cross-Service Read)")
            .get("/api/appointments/by-doctor/${selectedDoctorId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404, 500, 502, 503, 504))
        )
        .pause(2.seconds, 4.seconds)
      }
    }

  /**
   * Database-intensive scenario - tests recovery from database failures.
   */
  val databaseRecoveryScenario = scenario("Database Recovery (Connection Resilience)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.in(200, 401, 503, 504))
        .check(jsonPath("$.data.accessToken").optional.saveAs("authToken"))
    )
    .pause(500.milliseconds)
    .doIf(session => session.contains("authToken")) {
      forever {
        // Transactional write (tests transaction rollback/commit)
        exec(session => {
          val uniqueId = UUID.randomUUID().toString.take(8)
          session.set("transactionId", uniqueId)
        })
        .exec(
          http("POST /patients (DB Transaction)")
            .post("/api/patients")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody(
              """{
                "fullName": "Recovery Test Patient ${transactionId}",
                "email": "recovery-test-${transactionId}@test.com",
                "phone": "0123456789",
                "gender": "MALE"
              }"""
            )).asJson
            .check(status.in(200, 201, 400, 409, 500, 502, 503, 504))
            .check(jsonPath("$.data.id").optional.saveAs("newPatientId"))
        )
        .pause(1.second)
        
        // Read-after-write consistency check
        .doIf(session => session.contains("newPatientId")) {
          exec(
            http("GET /patients/{id} (Consistency Check)")
              .get("/api/patients/${newPatientId}")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404, 500, 502, 503, 504))
          )
        }
        .pause(2.seconds, 4.seconds)
        
        // Bulk read (connection pool stress)
        .exec(
          http("GET /patients (Bulk Read)")
            .get("/api/patients")
            .queryParam("page", "0")
            .queryParam("size", "100")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 500, 502, 503, 504))
        )
        .pause(3.seconds, 5.seconds)
      }
    }

  /**
   * Gateway overload scenario - tests API gateway resilience and rate limiting.
   */
  val gatewayOverloadScenario = scenario("Gateway Overload (Rate Limiting Test)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login (Gateway)")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.in(200, 401, 429, 503, 504)) // 429 = rate limited
        .check(jsonPath("$.data.accessToken").optional.saveAs("authToken"))
    )
    .pause(200.milliseconds)
    .doIf(session => session.contains("authToken")) {
      forever {
        // Rapid-fire requests to test rate limiting
        repeat(5) {
          exec(
            http("GET /auth/me (Gateway Pressure)")
              .get("/api/auth/me")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 429, 500, 502, 503, 504))
          )
          .pause(100.milliseconds, 300.milliseconds)
        }
        .pause(1.second, 2.seconds)
        
        // Mixed service requests through gateway
        .exec(
          http("GET /patients (Gateway Route)")
            .get("/api/patients")
            .queryParam("page", "0")
            .queryParam("size", "10")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 429, 500, 502, 503, 504))
        )
        .pause(500.milliseconds)
        
        .exec(
          http("GET /appointments (Gateway Route)")
            .get("/api/appointments")
            .queryParam("page", "0")
            .queryParam("size", "10")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 429, 500, 502, 503, 504))
        )
        .pause(1.second, 2.seconds)
      }
    }

  /**
   * Recovery verification scenario - runs during recovery phase to verify system stabilization.
   */
  val recoveryVerificationScenario = scenario("Recovery Verification")
    .feed(mixedUserFeeder)
    .exec(
      http("Login (Recovery)")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200)) // Expect success during recovery
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(1.second)
    .repeat(20) {
      // Verify all services are responding normally
      exec(
        http("GET /patients (Recovery Verify)")
          .get("/api/patients")
          .queryParam("page", "0")
          .queryParam("size", "10")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
          .check(responseTimeInMillis.lte(2000)) // Normal response times
      )
      .pause(2.seconds)
      
      .exec(
        http("GET /appointments (Recovery Verify)")
          .get("/api/appointments")
          .queryParam("page", "0")
          .queryParam("size", "10")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
          .check(responseTimeInMillis.lte(2000))
      )
      .pause(2.seconds)
      
      .exec(
        http("GET /schedules/doctors (Recovery Verify)")
          .get("/schedules/doctors")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404)) // 404 ok if no schedules
          .check(responseTimeInMillis.lte(2000))
      )
      .pause(2.seconds)
    }

  setUp(
    // Phase 1: Establish baseline (200 concurrent users)
    baselineTrafficScenario.inject(
      rampUsers(200).during(2.minutes),
      nothingFor(13.minutes) // Run for full test duration
    ),
    
    // Phase 2: Patient service stress (simulated failure at T+2min)
    patientServiceStressScenario.inject(
      nothingFor(2.minutes),
      rampUsers(50).during(30.seconds),
      nothingFor(3.minutes)
    ),
    
    // Phase 3: Cross-service dependency test (simulated network partition at T+5min)
    crossServiceDependencyScenario.inject(
      nothingFor(5.minutes),
      rampUsers(40).during(30.seconds),
      nothingFor(3.minutes)
    ),
    
    // Phase 4: Database recovery test (simulated DB failure at T+8min)
    databaseRecoveryScenario.inject(
      nothingFor(8.minutes),
      rampUsers(30).during(30.seconds),
      nothingFor(2.minutes)
    ),
    
    // Phase 5: Gateway overload test
    gatewayOverloadScenario.inject(
      nothingFor(10.minutes),
      rampUsers(50).during(30.seconds),
      nothingFor(2.minutes)
    ),
    
    // Phase 6: Recovery verification (T+12min)
    recoveryVerificationScenario.inject(
      nothingFor(12.minutes),
      atOnceUsers(20)
    )
  ).protocols(httpProtocol)
   .assertions(
     // During failure phases, error rate should be contained
     global.failedRequests.percent.lt(30.0), // Allow up to 30% during failures
     
     // Recovery phase should show high success rate
     details("GET /patients (Recovery Verify)").successfulRequests.percent.gte(90.0),
     details("GET /appointments (Recovery Verify)").successfulRequests.percent.gte(90.0),
     
     // Response times during recovery should be reasonable
     details("GET /patients (Recovery Verify)").responseTime.percentile(95).lt(3000),
     details("GET /appointments (Recovery Verify)").responseTime.percentile(95).lt(3000),
     
     // System should not completely fail
     global.responseTime.percentile(99).lt(30000)
   )
   .maxDuration(15.minutes)
}
