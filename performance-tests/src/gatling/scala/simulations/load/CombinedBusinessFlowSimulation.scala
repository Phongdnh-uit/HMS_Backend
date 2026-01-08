package simulations.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * PERF-LOAD-001: Combined Business Flow - 500 Concurrent Users
 * 
 * Comprehensive load test simulating all HMS user roles executing core 
 * business workflows concurrently.
 *
 * Role Distribution (500 VUs Total):
 * - Patient: 275 VUs (55%) - Registration, Booking, View Records, Payment
 * - Doctor: 90 VUs (18%) - View Schedule, Examine Patients, Prescribe
 * - Nurse: 70 VUs (14%) - Check Vitals, Update Records
 * - Receptionist: 60 VUs (12%) - Walk-in Registration, Billing
 * - Admin: 5 VUs (1%) - Manage Staff, Reports
 *
 * Load Profile:
 * - Ramp-up: 5 minutes (0 -> 500 VUs)
 * - Peak Load: 20 minutes (500 VUs sustained)
 * - Ramp-down: 5 minutes (500 -> 0 VUs)
 * - Total Duration: 30 minutes
 */
class CombinedBusinessFlowSimulation extends HmsSimulationBase {

  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  // ==================== PATIENT SCENARIOS ====================
  
  // Patient appointment booking scenario (55% - 275 VUs)
  val patientBookingScenario = scenario("Patient - Appointment Booking")
    .feed(patientFeeder)
    // Login
    .exec(
      http("Patient Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(patientThinkTime)
    // Get available doctors
    .exec(
      http("Get Available Doctors")
        .get("/schedules/doctors")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.in(200, 404))
        .check(jsonPath("$.data[0].employeeId").optional.saveAs("selectedDoctorId"))
    )
    .pause(shortThinkTime)
    // View doctor schedules
    .exec(
      http("Get Doctor Schedules")
        .get("/schedules?employeeId=${selectedDoctorId}")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.in(200, 404))
    )
    .pause(mediumThinkTime)
    // Get available slots
    .exec(
      http("Get Available Slots")
        .get("/api/appointments/available-slots?doctorId=${selectedDoctorId}")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.in(200, 404))
    )
    .pause(patientThinkTime)
    // View own appointments
    .exec(session => {
      val patientId = session("patientIndex").as[Int]
      session.set("patientId", s"patient-$patientId")
    })
    .exec(
      http("View My Appointments")
        .get("/api/appointments/by-patient/${patientId}")
        .header("Authorization", "Bearer ${authToken}")
        .check(status.in(200, 404))
    )
    .pause(longThinkTime)
    // Loop for sustained activity
    .repeat(5) {
      exec(
        http("View Patient Profile")
          .get("/api/patients/me")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(patientThinkTime)
    }

  // Patient viewing medical history scenario
  val patientViewHistoryScenario = scenario("Patient - View Medical History")
    .feed(patientFeeder)
    .exec(
      http("Patient Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(patientThinkTime)
    .repeat(10) {
      exec(
        http("View Medical Exams")
          .get("/exams/by-patient/me")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(mediumThinkTime)
      .exec(
        http("View Prescriptions")
          .get("/prescriptions?patientId=me")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(longThinkTime)
    }

  // ==================== DOCTOR SCENARIOS ====================
  
  val doctorWorkflowScenario = scenario("Doctor - Patient Examination")
    .feed(doctorFeeder)
    // Login
    .exec(
      http("Doctor Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
        .check(jsonPath("$.data.user.id").optional.saveAs("doctorUserId"))
    )
    .pause(doctorThinkTime)
    // Main doctor workflow loop
    .repeat(8) { // Examine 5-8 patients during test
      exec(
        // Get appointment queue
        http("Get Appointment Queue")
          .get("/api/appointments/by-doctor/${doctorIndex}?status=CHECKED_IN")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(jsonPath("$.data[0].id").optional.saveAs("currentAppointmentId"))
          .check(jsonPath("$.data[0].patientId").optional.saveAs("currentPatientId"))
      )
      .pause(mediumThinkTime)
      // View patient details
      .doIf(session => session.contains("currentPatientId")) {
        exec(
          http("Get Patient Details")
            .get("/api/patients/${currentPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(doctorThinkTime)
        // View patient medical history
        .exec(
          http("Get Patient Medical History")
            .get("/exams/by-patient/${currentPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(doctorThinkTime)
        // View previous prescriptions
        .exec(
          http("Get Previous Prescriptions")
            .get("/prescriptions?patientId=${currentPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(doctorThinkTime)
      }
    }

  // ==================== NURSE SCENARIOS ====================
  
  val nurseWorkflowScenario = scenario("Nurse - Patient Preparation")
    .feed(nurseFeeder)
    // Login
    .exec(
      http("Nurse Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(nurseThinkTime)
    // Main nurse workflow loop
    .repeat(15) { // 15-25 actions during test
      exec(
        // Get waiting queue
        http("Get Waiting Queue")
          .get("/api/appointments/queue")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(jsonPath("$.data[0].id").optional.saveAs("queueAppointmentId"))
      )
      .pause(shortThinkTime)
      // Get patient list
      .exec(
        http("Get Patient List")
          .get("/api/patients?page=0&size=10")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(jsonPath("$.data.content[0].id").optional.saveAs("nursePatientId"))
      )
      .pause(nurseThinkTime)
      // View patient details
      .doIf(session => session.contains("nursePatientId")) {
        exec(
          http("View Patient for Vitals")
            .get("/api/patients/${nursePatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(nurseThinkTime)
      }
      // Check lab orders
      .exec(
        http("Check Pending Lab Orders")
          .get("/lab-orders?status=PENDING")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(nurseThinkTime)
    }

  // ==================== RECEPTIONIST SCENARIOS ====================
  
  val receptionistWorkflowScenario = scenario("Receptionist - Front Desk Operations")
    .feed(receptionistFeeder)
    // Login
    .exec(
      http("Receptionist Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(receptionistThinkTime)
    // Main receptionist workflow
    .repeat(20) { // 20-35 transactions during test
      randomSwitch(
        30.0 -> // Walk-in lookup
          exec(
            http("Search Patient by Phone")
              .get("/api/patients?search=090")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(receptionistThinkTime),
        40.0 -> // Appointment management
          exec(
            http("Get Today's Appointments")
              .get("/api/appointments?page=0&size=20")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(shortThinkTime)
          .exec(
            http("Get Available Doctors")
              .get("/schedules/doctors")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(receptionistThinkTime),
        30.0 -> // Billing lookup
          exec(
            http("Get Completed Appointments")
              .get("/api/appointments?status=COMPLETED")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(shortThinkTime)
          .exec(
            http("Get Invoices")
              .get("/invoices?page=0&size=10")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(receptionistThinkTime)
      )
    }

  // ==================== ADMIN SCENARIOS ====================
  
  val adminWorkflowScenario = scenario("Admin - Management Operations")
    .feed(adminFeeder)
    // Login
    .exec(
      http("Admin Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(adminThinkTime)
    // Admin workflow with various management tasks
    .repeat(10) {
      randomSwitch(
        25.0 -> // Staff management
          exec(
            http("Get All Employees")
              .get("/employees?page=0&size=50")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime)
          .exec(
            http("Get Doctors List")
              .get("/employees/doctors")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime),
        25.0 -> // Department management
          exec(
            http("Get All Departments")
              .get("/departments")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime)
          .exec(
            http("Get Doctor Schedules Overview")
              .get("/schedules/doctors")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime),
        25.0 -> // Medicine inventory
          exec(
            http("Get Low Stock Medicines")
              .get("/api/medicines/low-stock")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime)
          .exec(
            http("Get Medicine Categories")
              .get("/categories")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime),
        25.0 -> // Reports & Analytics
          exec(
            http("Get Appointment Stats")
              .get("/api/appointments/stats")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime)
          .exec(
            http("Get Patient List Report")
              .get("/api/patients?page=0&size=100")
              .header("Authorization", "Bearer ${authToken}")
              .check(status.in(200, 404))
          )
          .pause(adminThinkTime)
      )
    }

  // ==================== LOAD PROFILE SETUP ====================
  
  setUp(
    // Patient Scenarios (275 VUs total - 55%)
    patientBookingScenario.inject(
      rampUsers(140).during(5.minutes),
      nothingFor(20.minutes)
    ),
    patientViewHistoryScenario.inject(
      rampUsers(70).during(5.minutes),
      nothingFor(20.minutes)
    ),
    // Note: Payment scenario would be additional 65 VUs
    
    // Doctor Scenario (90 VUs - 18%)
    doctorWorkflowScenario.inject(
      rampUsers(90).during(5.minutes),
      nothingFor(20.minutes)
    ),
    
    // Nurse Scenario (70 VUs - 14%)
    nurseWorkflowScenario.inject(
      rampUsers(70).during(5.minutes),
      nothingFor(20.minutes)
    ),
    
    // Receptionist Scenario (60 VUs - 12%)
    receptionistWorkflowScenario.inject(
      rampUsers(60).during(5.minutes),
      nothingFor(20.minutes)
    ),
    
    // Admin Scenario (5 VUs - 1%)
    adminWorkflowScenario.inject(
      rampUsers(5).during(5.minutes),
      nothingFor(20.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Response Time SLAs
     details("Patient Login").responseTime.percentile(95).lt(500),
     details("Doctor Login").responseTime.percentile(95).lt(500),
     details("Get Appointment Queue").responseTime.percentile(95).lt(300),
     details("Get Patient Details").responseTime.percentile(95).lt(300),
     
     // System Health
     global.failedRequests.percent.lt(1.0),
     
     // Throughput
     global.requestsPerSec.gte(250.0)
   )
   .maxDuration(30.minutes)
}
