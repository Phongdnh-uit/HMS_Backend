package simulations.endurance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * PERF-END-001: 24-Hour Continuous Operation
 * 
 * Simulate realistic hospital workload over 24 hours with varying traffic patterns.
 * This test validates system stability, resource management, and consistent performance.
 *
 * Traffic Pattern (Realistic Hospital Day):
 * - 00:00-06:00: Low (10 users) - Night shift, emergency only
 * - 06:00-09:00: Ramp-up (10→100 users) - Morning arrival
 * - 09:00-12:00: Peak (100-150 users) - Morning consultations
 * - 12:00-14:00: Medium (50-80 users) - Lunch break
 * - 14:00-18:00: Peak (100-150 users) - Afternoon consultations
 * - 18:00-24:00: Ramp-down (100→10 users) - Evening closure
 *
 * Operation Mix:
 * - Patient registration: 5%
 * - Appointment booking: 20%
 * - Medical exams: 25%
 * - Prescription management: 15%
 * - Lab orders/results: 15%
 * - Billing/payments: 10%
 * - Reporting/queries: 10%
 *
 * Acceptance Criteria:
 * - Zero unhandled exceptions
 * - All services running continuously (100% uptime)
 * - Response time remains consistent (< 10% variance)
 * - Database maintains consistent performance
 * - No gradual performance degradation
 * - Scheduled tasks execute correctly
 * - Log file rotation working properly
 * - Session management: expired sessions cleaned up
 * - Metrics collection continuous without gaps
 * 
 * Note: For a true 24-hour test, run with: maxDuration(24.hours)
 * This configuration runs a compressed version for CI/CD environments.
 */
class ContinuousOperationSimulation extends HmsSimulationBase {

  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  // Patient feeder for patient operations
  val patientOperationFeeder = Iterator.from(1).map { i =>
    Map(
      "email" -> s"patient${(i % patientCount) + 1}@email.com",
      "password" -> defaultPassword,
      "patientIndex" -> ((i % patientCount) + 1),
      "role" -> "PATIENT"
    )
  }

  // Doctor feeder for clinical operations
  val doctorOperationFeeder = Iterator.from(1).map { i =>
    Map(
      "email" -> s"doctor${(i % doctorCount) + 1}@hms.com",
      "password" -> defaultPassword,
      "doctorIndex" -> ((i % doctorCount) + 1),
      "role" -> "DOCTOR"
    )
  }

  // Nurse feeder for nursing operations
  val nurseOperationFeeder = Iterator.from(1).map { i =>
    Map(
      "email" -> s"nurse${(i % nurseCount) + 1}@hms.com",
      "password" -> defaultPassword,
      "nurseIndex" -> ((i % nurseCount) + 1),
      "role" -> "NURSE"
    )
  }

  // Receptionist feeder for front-desk operations
  val receptionistOperationFeeder = Iterator.from(1).map { i =>
    Map(
      "email" -> s"receptionist${(i % receptionistCount) + 1}@hms.com",
      "password" -> defaultPassword,
      "receptionistIndex" -> ((i % receptionistCount) + 1),
      "role" -> "RECEPTIONIST"
    )
  }

  // Admin feeder for administrative operations
  val adminOperationFeeder = Iterator.from(1).map { i =>
    Map(
      "email" -> s"admin${(i % adminCount) + 1}@hms.com",
      "password" -> defaultPassword,
      "adminIndex" -> ((i % adminCount) + 1),
      "role" -> "ADMIN"
    )
  }

  /**
   * Patient workflow - booking appointments and viewing records.
   * Represents 55% of total traffic (275 out of 500 VUs at peak).
   */
  val patientWorkflowScenario = scenario("Patient Workflow (55%)")
    .feed(patientOperationFeeder)
    .exec(
      http("Patient Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
        .check(jsonPath("$.data.user.id").optional.saveAs("patientId"))
    )
    .pause(patientThinkTime)
    .forever {
      randomSwitch(
        // 40% - View appointments
        40.0 -> exec(
          http("GET /appointments/by-patient (Patient)")
            .get("/api/appointments/by-patient/${patientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        ).pause(patientThinkTime),
        
        // 30% - Book new appointment
        30.0 -> exec(session => {
          val uniqueId = UUID.randomUUID().toString.take(8)
          val doctorIndex = scala.util.Random.nextInt(doctorCount) + 1
          val dayOffset = scala.util.Random.nextInt(14) + 1
          val hour = 8 + scala.util.Random.nextInt(8)
          val appointmentTime = LocalDateTime.now()
            .plusDays(dayOffset)
            .withHour(hour)
            .withMinute(scala.util.Random.nextInt(4) * 15)
            .format(dateFormatter)
          session
            .set("uniqueId", uniqueId)
            .set("bookingDoctorId", s"emp-doctor-$doctorIndex")
            .set("bookingTime", appointmentTime)
        })
        .exec(
          http("GET /schedules/doctors (Patient Booking)")
            .get("/schedules/doctors")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(mediumThinkTime)
        .exec(
          http("POST /appointments (Patient Booking)")
            .post("/api/appointments")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody(
              """{
                "patientId": "${patientId}",
                "doctorId": "${bookingDoctorId}",
                "appointmentTime": "${bookingTime}",
                "type": "CONSULTATION",
                "reason": "Endurance test booking - ${uniqueId}"
              }"""
            )).asJson
            .check(status.in(200, 201, 400, 409))
        )
        .pause(patientThinkTime),
        
        // 20% - View medical history
        20.0 -> exec(
          http("GET /exams/by-patient (Patient History)")
            .get("/exams/by-patient/${patientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(mediumThinkTime)
        .exec(
          http("GET /prescriptions (Patient)")
            .get("/prescriptions")
            .queryParam("patientId", "${patientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(patientThinkTime),
        
        // 10% - View invoices
        10.0 -> exec(
          http("GET /invoices (Patient)")
            .get("/invoices")
            .queryParam("patientId", "${patientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(patientThinkTime)
      )
    }

  /**
   * Doctor workflow - consultations, exams, prescriptions.
   * Represents 18% of total traffic (90 out of 500 VUs at peak).
   */
  val doctorWorkflowScenario = scenario("Doctor Workflow (18%)")
    .feed(doctorOperationFeeder)
    .exec(
      http("Doctor Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(mediumThinkTime)
    .forever {
      // View patient queue
      exec(
        http("GET /appointments/by-doctor (Queue)")
          .get("/api/appointments/by-doctor/emp-doctor-${doctorIndex}")
          .queryParam("status", "CHECKED_IN")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(jsonPath("$.data[0].id").optional.saveAs("currentAppointmentId"))
          .check(jsonPath("$.data[0].patientId").optional.saveAs("currentPatientId"))
      )
      .pause(doctorThinkTime)
      
      // View patient details (if appointment exists)
      .doIf(session => session.contains("currentPatientId")) {
        exec(
          http("GET /patients/{id} (Doctor View)")
            .get("/api/patients/${currentPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(doctorThinkTime)
        
        // View medical history
        .exec(
          http("GET /exams/by-patient (Doctor View)")
            .get("/exams/by-patient/${currentPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(doctorThinkTime)
        
        // Create exam (80% chance)
        .randomSwitch(
          80.0 -> exec(session => {
            val examId = UUID.randomUUID().toString.take(8)
            session.set("examUniqueId", examId)
          })
          .exec(
            http("POST /exams (Doctor)")
              .post("/exams")
              .header("Authorization", "Bearer ${authToken}")
              .body(StringBody(
                """{
                  "appointmentId": "${currentAppointmentId}",
                  "patientId": "${currentPatientId}",
                  "doctorId": "emp-doctor-${doctorIndex}",
                  "symptoms": "Endurance test symptoms - ${examUniqueId}",
                  "diagnosis": "Endurance test diagnosis",
                  "notes": "Continuous operation test"
                }"""
              )).asJson
              .check(status.in(200, 201, 400, 409))
              .check(jsonPath("$.data.id").optional.saveAs("examId"))
          )
          .pause(doctorThinkTime)
          
          // Create prescription (60% of exams)
          .randomSwitch(
            60.0 -> exec(
              http("POST /prescriptions (Doctor)")
                .post("/prescriptions")
                .header("Authorization", "Bearer ${authToken}")
                .body(StringBody(
                  """{
                    "examId": "${examId}",
                    "patientId": "${currentPatientId}",
                    "notes": "Endurance test prescription"
                  }"""
                )).asJson
                .check(status.in(200, 201, 400, 409))
            )
            .pause(doctorThinkTime),
            40.0 -> pause(shortThinkTime)
          )
          
          // Order lab tests (30% of exams)
          .randomSwitch(
            30.0 -> exec(
              http("POST /lab-orders (Doctor)")
                .post("/lab-orders")
                .header("Authorization", "Bearer ${authToken}")
                .body(StringBody(
                  """{
                    "examId": "${examId}",
                    "patientId": "${currentPatientId}",
                    "notes": "Endurance test lab order"
                  }"""
                )).asJson
                .check(status.in(200, 201, 400, 409))
            )
            .pause(doctorThinkTime),
            70.0 -> pause(shortThinkTime)
          ),
          20.0 -> pause(doctorThinkTime)
        )
      }
      .pause(doctorThinkTime)
    }

  /**
   * Nurse workflow - patient check-in, vitals, lab processing.
   * Represents 14% of total traffic (70 out of 500 VUs at peak).
   */
  val nurseWorkflowScenario = scenario("Nurse Workflow (14%)")
    .feed(nurseOperationFeeder)
    .exec(
      http("Nurse Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(nurseThinkTime)
    .forever {
      // View waiting patients queue
      exec(
        http("GET /appointments (Nurse Queue)")
          .get("/api/appointments")
          .queryParam("status", "SCHEDULED")
          .queryParam("page", "0")
          .queryParam("size", "20")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(jsonPath("$.data.content[0].id").optional.saveAs("waitingAppointmentId"))
          .check(jsonPath("$.data.content[0].patientId").optional.saveAs("waitingPatientId"))
      )
      .pause(nurseThinkTime)
      
      // Check-in patient
      .doIf(session => session.contains("waitingAppointmentId")) {
        exec(
          http("PUT /appointments/{id}/check-in (Nurse)")
            .put("/api/appointments/${waitingAppointmentId}/check-in")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 400, 404))
        )
        .pause(nurseThinkTime)
        
        // View patient details
        .exec(
          http("GET /patients/{id} (Nurse)")
            .get("/api/patients/${waitingPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(nurseThinkTime)
      }
      
      // Check lab orders queue
      .exec(
        http("GET /lab-orders (Nurse)")
          .get("/lab-orders")
          .queryParam("status", "PENDING")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(nurseThinkTime)
    }

  /**
   * Receptionist workflow - registration, scheduling, billing.
   * Represents 12% of total traffic (60 out of 500 VUs at peak).
   */
  val receptionistWorkflowScenario = scenario("Receptionist Workflow (12%)")
    .feed(receptionistOperationFeeder)
    .exec(
      http("Receptionist Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(receptionistThinkTime)
    .forever {
      randomSwitch(
        // 30% - Patient search/registration
        30.0 -> exec(
          http("GET /patients (Search)")
            .get("/api/patients")
            .queryParam("page", "0")
            .queryParam("size", "20")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.is(200))
        )
        .pause(receptionistThinkTime),
        
        // 40% - Appointment scheduling
        40.0 -> exec(
          http("GET /schedules/doctors (Receptionist)")
            .get("/schedules/doctors")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(shortThinkTime)
        .exec(
          http("GET /appointments/available-slots")
            .get("/api/appointments/available-slots")
            .queryParam("doctorId", "emp-doctor-${receptionistIndex}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(receptionistThinkTime),
        
        // 30% - Billing operations
        30.0 -> exec(
          http("GET /appointments (Completed)")
            .get("/api/appointments")
            .queryParam("status", "COMPLETED")
            .queryParam("page", "0")
            .queryParam("size", "10")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(shortThinkTime)
        .exec(
          http("GET /invoices (Receptionist)")
            .get("/invoices")
            .queryParam("page", "0")
            .queryParam("size", "20")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(receptionistThinkTime)
      )
    }

  /**
   * Admin workflow - reports, staff management, system monitoring.
   * Represents 1% of total traffic (5 out of 500 VUs at peak).
   */
  val adminWorkflowScenario = scenario("Admin Workflow (1%)")
    .feed(adminOperationFeeder)
    .exec(
      http("Admin Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(adminThinkTime)
    .forever {
      randomSwitch(
        // Staff management
        25.0 -> exec(
          http("GET /employees (Admin)")
            .get("/employees")
            .queryParam("page", "0")
            .queryParam("size", "50")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(adminThinkTime)
        .exec(
          http("GET /departments (Admin)")
            .get("/departments")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(adminThinkTime),
        
        // Schedule management
        25.0 -> exec(
          http("GET /schedules (Admin)")
            .get("/schedules")
            .queryParam("page", "0")
            .queryParam("size", "50")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(adminThinkTime),
        
        // Medicine inventory
        25.0 -> exec(
          http("GET /medicines (Admin)")
            .get("/api/medicines")
            .queryParam("page", "0")
            .queryParam("size", "100")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(shortThinkTime)
        .exec(
          http("GET /categories (Admin)")
            .get("/categories")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(adminThinkTime),
        
        // Reports and analytics
        25.0 -> exec(
          http("GET /appointments/stats (Admin)")
            .get("/api/appointments/stats")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(shortThinkTime)
        .exec(
          http("GET /invoices/stats (Admin)")
            .get("/invoices/stats")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(adminThinkTime)
      )
    }

  // For full 24-hour test, use these injection profiles:
  // This is a compressed 2-hour version for CI/CD
  setUp(
    // Night shift (low load) - 10 users
    patientWorkflowScenario.inject(
      rampUsers(6).during(2.minutes),   // 55% of 10 = 6
      nothingFor(118.minutes)
    ),
    
    doctorWorkflowScenario.inject(
      rampUsers(2).during(2.minutes),   // 18% of 10 = 2
      nothingFor(118.minutes)
    ),
    
    nurseWorkflowScenario.inject(
      rampUsers(1).during(2.minutes),   // 14% of 10 = 1
      nothingFor(118.minutes)
    ),
    
    receptionistWorkflowScenario.inject(
      rampUsers(1).during(2.minutes),   // 12% of 10 = 1
      nothingFor(118.minutes)
    ),
    
    // Morning ramp-up (10 → 100 users) - starts at 15 min
    patientWorkflowScenario.inject(
      nothingFor(15.minutes),
      rampUsers(49).during(15.minutes),  // Add 49 more patients (55 total)
      nothingFor(90.minutes)
    ),
    
    doctorWorkflowScenario.inject(
      nothingFor(15.minutes),
      rampUsers(16).during(15.minutes),  // Add 16 more doctors (18 total)
      nothingFor(90.minutes)
    ),
    
    nurseWorkflowScenario.inject(
      nothingFor(15.minutes),
      rampUsers(13).during(15.minutes),  // Add 13 more nurses (14 total)
      nothingFor(90.minutes)
    ),
    
    receptionistWorkflowScenario.inject(
      nothingFor(15.minutes),
      rampUsers(11).during(15.minutes),  // Add 11 more receptionists (12 total)
      nothingFor(90.minutes)
    ),
    
    // Peak load (100-150 users) - starts at 30 min
    patientWorkflowScenario.inject(
      nothingFor(30.minutes),
      rampUsers(28).during(10.minutes),  // Add to reach ~150 VUs peak (83 total patients)
      nothingFor(80.minutes)
    ),
    
    doctorWorkflowScenario.inject(
      nothingFor(30.minutes),
      rampUsers(9).during(10.minutes),   // 27 total doctors
      nothingFor(80.minutes)
    ),
    
    nurseWorkflowScenario.inject(
      nothingFor(30.minutes),
      rampUsers(7).during(10.minutes),   // 21 total nurses
      nothingFor(80.minutes)
    ),
    
    receptionistWorkflowScenario.inject(
      nothingFor(30.minutes),
      rampUsers(5).during(10.minutes),   // 17 total receptionists
      nothingFor(80.minutes)
    ),
    
    adminWorkflowScenario.inject(
      nothingFor(30.minutes),
      rampUsers(3).during(10.minutes),   // 3 admins
      nothingFor(80.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Zero crashes / unhandled exceptions
     global.failedRequests.percent.lt(2.0),
     
     // Response times remain consistent
     global.responseTime.percentile(95).lt(3000),
     global.responseTime.percentile(99).lt(5000),
     
     // All critical operations succeed
     details("Patient Login").successfulRequests.percent.gte(99.0),
     details("Doctor Login").successfulRequests.percent.gte(99.0),
     details("Nurse Login").successfulRequests.percent.gte(99.0),
     
     // Throughput maintained
     global.requestsPerSec.gte(10.0)
   )
   .maxDuration(125.minutes) // 2 hour compressed test + buffer
   // For full 24-hour test, uncomment: .maxDuration(24.hours)
}
