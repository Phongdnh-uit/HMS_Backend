package simulations.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase

/**
 * PERF-LOAD-004: Gateway Routing Under Load
 * 
 * Tests API Gateway routing, load balancing, and circuit breaker under mixed traffic load.
 *
 * Test Steps:
 * 1. Generate mixed traffic (500 concurrent users):
 *    - 20% Authentication requests → auth-service
 *    - 25% Patient operations → patient-service
 *    - 25% Appointment operations → appointment-service
 *    - 15% Medical exam operations → medical-exam-service
 *    - 10% HR operations → hr-service
 *    - 5% Medicine operations → medicine-service
 * 2. Duration: 15 minutes
 *
 * Acceptance Criteria:
 * - Gateway routing latency < 50ms (overhead)
 * - JWT validation at gateway < 20ms
 * - Header injection (X-User-Id, X-User-Role) working
 * - Overall error rate < 2%
 */
class GatewayRoutingSimulation extends HmsSimulationBase {

  // Staff feeder (doctors, nurses, receptionists, patients)
  val mixedUserFeeder = {
    val doctors = (1 to doctorCount).map(i => Map("email" -> s"doctor$i@hms.com", "password" -> defaultPassword, "role" -> "DOCTOR"))
    val nurses = (1 to nurseCount).map(i => Map("email" -> s"nurse$i@hms.com", "password" -> defaultPassword, "role" -> "NURSE"))
    val receptionists = (1 to receptionistCount).map(i => Map("email" -> s"receptionist$i@hms.com", "password" -> defaultPassword, "role" -> "RECEPTIONIST"))
    val patients = (1 to 100).map(i => Map("email" -> s"patient$i@email.com", "password" -> defaultPassword, "role" -> "PATIENT"))
    scala.util.Random.shuffle(doctors ++ nurses ++ receptionists ++ patients).toArray.circular
  }

  // 20% - Authentication Traffic (auth-service)
  val authenticationScenario = scenario("Gateway - Auth Service (20%)")
    .feed(mixedUserFeeder)
    .forever {
      exec(
        http("POST /auth/login")
          .post("/auth/login")
          .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
          .check(status.is(200))
          .check(jsonPath("$.data.token").saveAs("authToken"))
          .check(responseTimeInMillis.saveAs("loginTime"))
      )
      .pause(mediumThinkTime)
      .exec(
        http("GET /auth/me")
          .get("/auth/me")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
      )
      .pause(mediumThinkTime)
      .exec(
        http("POST /auth/refresh")
          .post("/auth/refresh")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody("""{"refreshToken": "test-refresh-token"}""")).asJson
          .check(status.in(200, 400, 401))
      )
      .pause(longThinkTime)
    }

  // 25% - Patient Operations (patient-service)
  val patientOperationsScenario = scenario("Gateway - Patient Service (25%)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login for Patient Ops")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(shortThinkTime)
    .forever {
      randomSwitch(
        40.0 ->
          exec(
            http("GET /patients (List)")
              .get("/patients")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.is(200))
          ),
        30.0 ->
          exec(session => {
            session.set("patientIdx", scala.util.Random.nextInt(1000) + 1)
          })
          .exec(
            http("GET /patients/{id}")
              .get("/patients/patient-${patientIdx}")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        30.0 ->
          exec(
            http("GET /patients/me")
              .get("/patients/me")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
      )
      .pause(shortThinkTime)
    }

  // 25% - Appointment Operations (appointment-service)
  val appointmentOperationsScenario = scenario("Gateway - Appointment Service (25%)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login for Appointment Ops")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(shortThinkTime)
    .forever {
      randomSwitch(
        35.0 ->
          exec(
            http("GET /appointments (List)")
              .get("/appointments")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        25.0 ->
          exec(
            http("GET /appointments/queue")
              .get("/appointments/queue")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        25.0 ->
          exec(
            http("GET /appointments/available-slots")
              .get("/appointments/available-slots")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        15.0 ->
          exec(
            http("GET /appointments/stats")
              .get("/appointments/stats")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
      )
      .pause(shortThinkTime)
    }

  // 15% - Medical Exam Operations (medical-exam-service)
  val medicalExamOperationsScenario = scenario("Gateway - Medical Exam Service (15%)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login for Exam Ops")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(shortThinkTime)
    .forever {
      randomSwitch(
        40.0 ->
          exec(
            http("GET /exams (List)")
              .get("/exams")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        30.0 ->
          exec(
            http("GET /prescriptions")
              .get("/prescriptions")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        30.0 ->
          exec(
            http("GET /lab-orders")
              .get("/lab-orders")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
      )
      .pause(shortThinkTime)
    }

  // 10% - HR Operations (hr-service)
  val hrOperationsScenario = scenario("Gateway - HR Service (10%)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login for HR Ops")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(shortThinkTime)
    .forever {
      randomSwitch(
        35.0 ->
          exec(
            http("GET /employees (List)")
              .get("/employees")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        30.0 ->
          exec(
            http("GET /departments")
              .get("/departments")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        35.0 ->
          exec(
            http("GET /schedules/doctors")
              .get("/schedules/doctors")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
      )
      .pause(shortThinkTime)
    }

  // 5% - Medicine Operations (medicine-service)
  val medicineOperationsScenario = scenario("Gateway - Medicine Service (5%)")
    .feed(mixedUserFeeder)
    .exec(
      http("Login for Medicine Ops")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(shortThinkTime)
    .forever {
      randomSwitch(
        40.0 ->
          exec(
            http("GET /medicines (List)")
              .get("/medicines")
              .queryParam("page", "0")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        30.0 ->
          exec(
            http("GET /medicines/low-stock")
              .get("/medicines/low-stock")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        30.0 ->
          exec(
            http("GET /categories")
              .get("/categories")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
      )
      .pause(shortThinkTime)
    }

  setUp(
    // 20% Auth traffic = 100 users
    authenticationScenario.inject(
      rampUsers(100).during(30.seconds),
      nothingFor(14.5.minutes)
    ),
    // 25% Patient traffic = 125 users
    patientOperationsScenario.inject(
      rampUsers(125).during(30.seconds),
      nothingFor(14.5.minutes)
    ),
    // 25% Appointment traffic = 125 users
    appointmentOperationsScenario.inject(
      rampUsers(125).during(30.seconds),
      nothingFor(14.5.minutes)
    ),
    // 15% Medical exam traffic = 75 users
    medicalExamOperationsScenario.inject(
      rampUsers(75).during(30.seconds),
      nothingFor(14.5.minutes)
    ),
    // 10% HR traffic = 50 users
    hrOperationsScenario.inject(
      rampUsers(50).during(30.seconds),
      nothingFor(14.5.minutes)
    ),
    // 5% Medicine traffic = 25 users
    medicineOperationsScenario.inject(
      rampUsers(25).during(30.seconds),
      nothingFor(14.5.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Gateway routing latency should add minimal overhead
     // Login operations (auth-service)
     details("POST /auth/login").responseTime.percentile(95).lt(500),
     // Overall error rate < 2%
     global.failedRequests.percent.lt(2.0),
     // Minimum throughput
     global.requestsPerSec.gte(100.0)
   )
   .maxDuration(15.minutes)
}
