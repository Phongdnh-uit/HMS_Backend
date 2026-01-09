package simulations.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase

/**
 * PERF-LOAD-001: 100 Concurrent User Logins
 * 
 * Simulates morning peak hours when hospital staff (doctors, nurses, receptionists) 
 * log in simultaneously.
 *
 * Test Steps:
 * 1. Prepare 100 valid user accounts (mixed roles: 30 doctors, 30 nurses, 20 receptionists, 20 patients)
 * 2. Execute concurrent login requests via API Gateway
 * 3. Each thread performs: POST /auth/login → Validate JWT → GET /auth/me
 * 4. Ramp-up: 0 to 100 users in 10 seconds
 * 5. Sustain load for 5 minutes
 * 6. Ramp-down: 100 to 0 in 10 seconds
 *
 * Acceptance Criteria:
 * - 95th percentile response time < 500ms
 * - 99th percentile response time < 1000ms
 * - Error rate < 1%
 * - Successful JWT generation for all valid logins
 */
class ConcurrentLoginsSimulation extends HmsSimulationBase {

  // Feeder with role distribution: 30 doctors, 30 nurses, 20 receptionists, 20 patients
  val loginFeeder = {
    val doctors = (1 to 30).map(i => Map("email" -> s"doctor$i@hms.com", "password" -> defaultPassword, "role" -> "DOCTOR"))
    val nurses = (1 to 30).map(i => Map("email" -> s"nurse$i@hms.com", "password" -> defaultPassword, "role" -> "NURSE"))
    val receptionists = (1 to 20).map(i => Map("email" -> s"receptionist$i@hms.com", "password" -> defaultPassword, "role" -> "RECEPTIONIST"))
    val patients = (1 to 20).map(i => Map("email" -> s"patient$i@email.com", "password" -> defaultPassword, "role" -> "PATIENT"))
    
    // Shuffle for random distribution
    scala.util.Random.shuffle(doctors ++ nurses ++ receptionists ++ patients).toArray.circular
  }

  // Login scenario with JWT validation
  val loginScenario = scenario("Concurrent Login Scenario")
    .feed(loginFeeder)
    .exec(
      http("POST /auth/login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").exists.saveAs("authToken"))
        .check(responseTimeInMillis.lte(1000))
    )
    .pause(shortThinkTime)
    .exec(
      http("GET /auth/me")
        .get("/api/auth/me")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.is(200))
    )

  // Repeat login scenario for sustained load
  val sustainedLoginScenario = scenario("Sustained Login Load")
    .forever {
      feed(loginFeeder)
      .exec(
        http("POST /auth/login")
          .post("/api/auth/login")
          .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
          .check(status.is(200))
          .check(jsonPath("$.data.accessToken").exists.saveAs("authToken"))
      )
      .pause(mediumThinkTime)
      .exec(
        http("GET /auth/me")
          .get("/api/auth/me")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
      )
      .pause(mediumThinkTime)
    }

  setUp(
    sustainedLoginScenario.inject(
      // Ramp-up: 0 to 100 users in 10 seconds
      rampUsers(100).during(10.seconds),
      // Sustain: 100 users for 5 minutes
      nothingFor(5.minutes)
    ).throttle(
      reachRps(50).in(10.seconds),
      holdFor(5.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // 95th percentile response time < 500ms
     global.responseTime.percentile(95).lt(500),
     // 99th percentile response time < 1000ms
     global.responseTime.percentile(99).lt(1000),
     // Error rate < 1%
     global.failedRequests.percent.lt(1.0),
     // All successful logins
     details("POST /auth/login").successfulRequests.percent.gte(99.0)
   )
   .maxDuration(6.minutes)
}
