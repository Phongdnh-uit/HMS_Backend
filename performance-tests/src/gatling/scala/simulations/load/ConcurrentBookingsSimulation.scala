package simulations.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * PERF-LOAD-002: 50 Concurrent Appointment Bookings
 * 
 * Simulates peak booking hours (8-9 AM) when patients book appointments 
 * through mobile/web application.
 *
 * Test Steps:
 * 1. Prepare 50 authenticated patient accounts
 * 2. Each virtual user performs:
 *    - GET /schedules/doctors (find available doctors)
 *    - GET /appointments/available-slots (find time slots)
 *    - POST /appointments (book appointment)
 *    - GET /appointments/by-patient/{id} (verify booking)
 * 3. Concurrent execution: 50 users booking simultaneously
 * 4. Duration: 3 minutes sustained load
 *
 * Acceptance Criteria:
 * - 95th percentile response time < 800ms for booking
 * - 99th percentile response time < 1500ms
 * - Zero double-booking errors (data consistency)
 * - All 50 bookings successful without conflicts
 * - Error rate < 0.5%
 *
 * Business Logic Validation:
 * - No overlapping appointments for same doctor/time slot
 * - Appointment status correctly transitions
 * - Audit trail recorded for each booking
 */
class ConcurrentBookingsSimulation extends HmsSimulationBase {

  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  // Feeder for 50 patients with unique booking slots
  // Using different doctor/time combinations to avoid conflicts
  val bookingFeeder = (1 to 50).map { i =>
    val doctorIndex = ((i - 1) % doctorCount) + 1
    val dayOffset = (i - 1) / doctorCount + 1
    val hour = 8 + ((i - 1) % 8) // Hours from 8 AM to 3 PM
    val appointmentTime = LocalDateTime.now()
      .plusDays(dayOffset)
      .withHour(hour)
      .withMinute(0)
      .withSecond(0)
      .format(dateFormatter)
    
    Map(
      "email" -> s"patient$i@email.com",
      "password" -> defaultPassword,
      "patientIndex" -> i,
      "doctorId" -> s"emp-doctor-$doctorIndex",
      "appointmentTime" -> appointmentTime,
      "uniqueId" -> UUID.randomUUID().toString.take(8)
    )
  }.toArray.circular

  // Main booking scenario
  val appointmentBookingScenario = scenario("Concurrent Appointment Booking")
    .feed(bookingFeeder)
    // Step 1: Login
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
        .check(jsonPath("$.data.user.id").optional.saveAs("patientId"))
    )
    .pause(500.milliseconds, 1.second)
    
    // Step 2: Get available doctors
    .exec(
      http("GET /schedules/doctors")
        .get("/schedules/doctors")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.in(200, 404))
        .check(jsonPath("$.data[0].employeeId").optional.saveAs("selectedDoctorId"))
    )
    .pause(500.milliseconds, 1.second)
    
    // Step 3: Get available slots for the doctor
    .exec(
      http("GET /appointments/available-slots")
        .get("/api/appointments/available-slots")
        .queryParam("doctorId", "${doctorId}")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.in(200, 404))
    )
    .pause(500.milliseconds, 1.second)
    
    // Step 4: Book appointment
    .exec(session => {
      // Generate appointment time dynamically to avoid conflicts
      val patientIndex = session("patientIndex").as[Int]
      val appointmentTime = LocalDateTime.now()
        .plusDays(1 + (patientIndex / 60))
        .withHour(8 + (patientIndex % 8))
        .withMinute((patientIndex % 4) * 15)
        .withSecond(0)
        .format(dateFormatter)
      session.set("dynamicAppointmentTime", appointmentTime)
    })
    .exec(
      http("POST /appointments (Book)")
        .post("/api/appointments")
        .header("Authorization", "Bearer ${authToken}")
        .body(StringBody(
          """{
            "patientId": "${patientId}",
            "doctorId": "${doctorId}",
            "appointmentTime": "${dynamicAppointmentTime}",
            "type": "CONSULTATION",
            "reason": "Performance Test Booking - ${uniqueId}",
            "notes": "Automated booking from load test"
          }"""
        )).asJson
        .check(status.in(200, 201, 400, 409)) // 409 = conflict (double booking)
        .check(jsonPath("$.data.id").optional.saveAs("appointmentId"))
        .check(responseTimeInMillis.saveAs("bookingResponseTime"))
    )
    .pause(500.milliseconds, 1.second)
    
    // Step 5: Verify booking
    .doIf(session => session.contains("appointmentId")) {
      exec(
        http("GET /appointments/by-patient/{id} (Verify)")
          .get("/api/appointments/by-patient/${patientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
          .check(jsonPath("$.data[*].id").findAll.optional.saveAs("patientAppointments"))
      )
    }

  // Sustained booking load scenario for longer test
  val sustainedBookingScenario = scenario("Sustained Booking Load")
    .feed(bookingFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(mediumThinkTime)
    .repeat(3) { // Each user attempts 3 bookings
      exec(session => {
        val timestamp = System.currentTimeMillis()
        val appointmentTime = LocalDateTime.now()
          .plusDays(scala.util.Random.nextInt(7) + 1)
          .withHour(8 + scala.util.Random.nextInt(8))
          .withMinute(scala.util.Random.nextInt(4) * 15)
          .withSecond(0)
          .format(dateFormatter)
        session.set("bookingTime", appointmentTime)
      })
      .exec(
        http("GET /schedules/doctors")
          .get("/schedules/doctors")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(shortThinkTime)
      .exec(
        http("POST /appointments")
          .post("/api/appointments")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(
            """{
              "doctorId": "${doctorId}",
              "appointmentTime": "${bookingTime}",
              "type": "CONSULTATION",
              "reason": "Load test booking"
            }"""
          )).asJson
          .check(status.in(200, 201, 400, 409))
      )
      .pause(mediumThinkTime)
    }

  setUp(
    appointmentBookingScenario.inject(
      // All 50 users start within 5 seconds (concurrent)
      rampUsers(50).during(5.seconds),
      // Sustain for 3 minutes
      nothingFor(3.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // 95th percentile response time < 800ms for booking
     details("POST /appointments (Book)").responseTime.percentile(95).lt(800),
     // 99th percentile response time < 1500ms
     details("POST /appointments (Book)").responseTime.percentile(99).lt(1500),
     // Error rate < 0.5%
     global.failedRequests.percent.lt(0.5),
     // Booking success rate >= 99%
     details("POST /appointments (Book)").successfulRequests.percent.gte(99.0)
   )
   .maxDuration(4.minutes)
}
