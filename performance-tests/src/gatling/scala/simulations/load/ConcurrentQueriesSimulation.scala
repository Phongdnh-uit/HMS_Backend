package simulations.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase

/**
 * PERF-LOAD-003: 1000 Concurrent Read Queries
 * 
 * Simulates multiple staff members querying patient records, appointments, 
 * and medical exam history concurrently.
 *
 * Test Steps:
 * 1. Execute mixed read operations:
 *    - 30% GET /patients?page=X&size=20 (paginated list)
 *    - 25% GET /appointments?status=X (filtered list)
 *    - 20% GET /exams/{id} (exam details)
 *    - 15% GET /patients/{id} (patient detail)
 *    - 10% GET /prescriptions/{id} (prescription details)
 * 2. Ramp-up: 0 to 1000 users in 30 seconds
 * 3. Sustain: 1000 concurrent users for 10 minutes
 * 4. Ramp-down: 30 seconds
 *
 * Acceptance Criteria:
 * - 95th percentile response time < 200ms
 * - 99th percentile response time < 500ms
 * - Throughput > 500 requests/second
 * - Error rate < 0.1%
 * - No database connection timeouts
 */
class ConcurrentQueriesSimulation extends HmsSimulationBase {

  // Mixed staff feeder for authentication (doctors, nurses, receptionists)
  val staffFeeder = {
    val doctors = (1 to doctorCount).map(i => Map("email" -> s"doctor$i@hms.com", "password" -> defaultPassword))
    val nurses = (1 to nurseCount).map(i => Map("email" -> s"nurse$i@hms.com", "password" -> defaultPassword))
    val receptionists = (1 to receptionistCount).map(i => Map("email" -> s"receptionist$i@hms.com", "password" -> defaultPassword))
    scala.util.Random.shuffle(doctors ++ nurses ++ receptionists).toArray.circular
  }

  // Page number feeder for pagination
  val pageFeeder = Iterator.from(0).map(i => Map("pageNum" -> (i % 50))) // Pages 0-49

  // Random patient ID feeder (from seed data: 1000 patients)
  val patientIdFeeder = Iterator.continually(Map(
    "randomPatientIndex" -> (scala.util.Random.nextInt(1000) + 1)
  ))

  // Appointment status feeder
  val statusFeeder = Iterator.continually(Map(
    "appointmentStatus" -> Seq("SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED")(scala.util.Random.nextInt(4))
  ))

  // Login once scenario - shared across all queries
  val loginOnce = exec(
    http("Staff Login")
      .post("/auth/login")
      .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
      .check(status.is(200))
      .check(jsonPath("$.data.token").saveAs("authToken"))
  )

  // 30% - Paginated Patient List Queries
  val paginatedPatientListScenario = scenario("Query - Paginated Patient List (30%)")
    .feed(staffFeeder)
    .feed(pageFeeder)
    .exec(loginOnce)
    .pause(shortThinkTime)
    .repeat(50) { // Each user makes 50 queries
      feed(pageFeeder)
      .exec(
        http("GET /patients (Paginated)")
          .get("/patients")
          .queryParam("page", "${pageNum}")
          .queryParam("size", "20")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
          .check(jsonPath("$.data.content").exists)
          .check(responseTimeInMillis.lte(500))
      )
      .pause(100.milliseconds, 500.milliseconds)
    }

  // 25% - Filtered Appointment List Queries
  val filteredAppointmentListScenario = scenario("Query - Filtered Appointments (25%)")
    .feed(staffFeeder)
    .feed(statusFeeder)
    .exec(loginOnce)
    .pause(shortThinkTime)
    .repeat(50) {
      feed(statusFeeder)
      .exec(
        http("GET /appointments (Filtered)")
          .get("/appointments")
          .queryParam("status", "${appointmentStatus}")
          .queryParam("page", "0")
          .queryParam("size", "20")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(responseTimeInMillis.lte(500))
      )
      .pause(100.milliseconds, 500.milliseconds)
    }

  // 20% - Exam Details Queries
  val examDetailsScenario = scenario("Query - Exam Details (20%)")
    .feed(staffFeeder)
    .exec(loginOnce)
    .pause(shortThinkTime)
    .repeat(50) {
      feed(patientIdFeeder)
      .exec(
        http("GET /exams/by-patient/{id}")
          .get("/exams/by-patient/patient-${randomPatientIndex}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(responseTimeInMillis.lte(500))
      )
      .pause(100.milliseconds, 500.milliseconds)
    }

  // 15% - Patient Details Queries
  val patientDetailsScenario = scenario("Query - Patient Details (15%)")
    .feed(staffFeeder)
    .exec(loginOnce)
    .pause(shortThinkTime)
    .repeat(50) {
      feed(patientIdFeeder)
      .exec(
        http("GET /patients/{id}")
          .get("/patients/patient-${randomPatientIndex}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(responseTimeInMillis.lte(300))
      )
      .pause(100.milliseconds, 500.milliseconds)
    }

  // 10% - Prescription Details Queries
  val prescriptionDetailsScenario = scenario("Query - Prescription Details (10%)")
    .feed(staffFeeder)
    .exec(loginOnce)
    .pause(shortThinkTime)
    .repeat(50) {
      feed(patientIdFeeder)
      .exec(
        http("GET /prescriptions")
          .get("/prescriptions")
          .queryParam("patientId", "patient-${randomPatientIndex}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(responseTimeInMillis.lte(500))
      )
      .pause(100.milliseconds, 500.milliseconds)
    }

  // Mixed query scenario for more realistic load distribution
  val mixedQueryScenario = scenario("Mixed Query Load")
    .feed(staffFeeder)
    .exec(loginOnce)
    .pause(shortThinkTime)
    .forever {
      randomSwitch(
        30.0 -> // Paginated patient list
          feed(pageFeeder)
          .exec(
            http("GET /patients (List)")
              .get("/patients")
              .queryParam("page", "${pageNum}")
              .queryParam("size", "20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.is(200))
          ),
        25.0 -> // Filtered appointments
          feed(statusFeeder)
          .exec(
            http("GET /appointments (Filtered)")
              .get("/appointments")
              .queryParam("status", "${appointmentStatus}")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        20.0 -> // Exam by patient
          feed(patientIdFeeder)
          .exec(
            http("GET /exams/by-patient")
              .get("/exams/by-patient/patient-${randomPatientIndex}")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        15.0 -> // Patient details
          feed(patientIdFeeder)
          .exec(
            http("GET /patients/{id}")
              .get("/patients/patient-${randomPatientIndex}")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          ),
        10.0 -> // Prescriptions
          feed(patientIdFeeder)
          .exec(
            http("GET /prescriptions")
              .get("/prescriptions")
              .queryParam("patientId", "patient-${randomPatientIndex}")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
      )
      .pause(50.milliseconds, 200.milliseconds)
    }

  setUp(
    mixedQueryScenario.inject(
      // Ramp-up: 0 to 1000 users in 30 seconds
      rampUsers(1000).during(30.seconds),
      // Sustain for 10 minutes (users will loop forever until duration ends)
      nothingFor(10.minutes)
    ).throttle(
      // Ensure minimum throughput of 500 req/s
      reachRps(500).in(30.seconds),
      holdFor(10.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // 95th percentile response time < 200ms
     global.responseTime.percentile(95).lt(200),
     // 99th percentile response time < 500ms
     global.responseTime.percentile(99).lt(500),
     // Throughput > 500 requests/second
     global.requestsPerSec.gte(500.0),
     // Error rate < 0.1%
     global.failedRequests.percent.lt(0.1)
   )
   .maxDuration(11.minutes)
}
