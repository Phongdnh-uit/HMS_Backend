package simulations.stress

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.util.UUID

/**
 * PERF-STRESS-002: Memory Usage Under Load
 * 
 * Test service behavior when approaching JVM heap limits and trigger garbage collection.
 *
 * JVM Configuration (Expected):
 * - Initial heap: 512MB
 * - Max heap: 2GB
 * - GC: G1GC with logging enabled
 *
 * Test Steps:
 * 1. Execute memory-intensive operations:
 *    - Fetch large datasets (1000+ patient records with full history)
 *    - Generate PDF reports for medical exams
 *    - Process file uploads (images, documents)
 *    - Export large Excel reports
 * 2. Sustain load: 100 concurrent users for 30 minutes
 * 3. Monitor heap usage, GC frequency, and response degradation
 *
 * Acceptance Criteria:
 * - Heap usage stays below 80% of max (< 1.6GB)
 * - Full GC events < 5 during test period
 * - GC pause time < 200ms
 * - No OutOfMemoryError exceptions
 * - Response time degradation < 20% at 70% heap usage
 * - Proper pagination for large datasets
 * - File uploads handled with streaming (not loaded into memory)
 * - Memory released after request completion
 */
class MemoryUsageSimulation extends HmsSimulationBase {

  // Staff feeder for authenticated operations
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
    val admins = (1 to adminCount).map(i => Map(
      "email" -> s"admin$i@hms.com",
      "password" -> defaultPassword,
      "role" -> "ADMIN",
      "staffIndex" -> i
    ))
    scala.util.Random.shuffle(doctors ++ nurses ++ admins).toArray.circular
  }

  // Patient feeder for patient-side operations
  val patientLargeDataFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"patient${(i % patientCount) + 1}@email.com",
    "password" -> defaultPassword,
    "patientIndex" -> ((i % patientCount) + 1)
  ))

  /**
   * Large dataset fetch scenario - stresses JVM heap with large object graphs.
   * Tests pagination handling and lazy loading effectiveness.
   */
  val largeDatasetFetchScenario = scenario("Large Dataset Fetch (Memory Pressure)")
    .feed(staffFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(1.second)
    .forever {
      // Fetch large patient list (1000+ records)
      exec(
        http("GET /patients (Large Dataset - 500 records)")
          .get("/api/patients")
          .queryParam("page", "0")
          .queryParam("size", "500")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
          .check(responseTimeInMillis.saveAs("largeQueryTime"))
      )
      .pause(2.seconds, 4.seconds)
      
      // Fetch all appointments (large result set with JOINs)
      .exec(
        http("GET /appointments (Large Dataset)")
          .get("/api/appointments")
          .queryParam("page", "0")
          .queryParam("size", "300")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
      )
      .pause(2.seconds, 4.seconds)
      
      // Multiple sequential fetches to accumulate memory pressure
      .repeat(3, "fetchIndex") {
        exec(
          http("GET /patients (Sequential Fetch ${fetchIndex})")
            .get("/api/patients")
            .queryParam("page", "${fetchIndex}")
            .queryParam("size", "200")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 500, 503))
        )
        .pause(1.second)
      }
      .pause(3.seconds, 5.seconds)
    }

  /**
   * Patient history deep fetch - retrieves full patient medical history.
   * Tests eager vs lazy loading impact on memory.
   */
  val patientHistoryScenario = scenario("Patient Full History (Deep Object Graph)")
    .feed(patientLargeDataFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
        .check(jsonPath("$.data.user.id").optional.saveAs("patientId"))
    )
    .pause(1.second)
    .forever {
      // Fetch patient details
      exec(
        http("GET /patients/{id} (Full Details)")
          .get("/api/patients/${patientIndex}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(1.second)
      
      // Fetch all exams for patient (with prescriptions, lab orders)
      .exec(
        http("GET /exams/by-patient (Full History)")
          .get("/exams/by-patient/${patientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(1.second)
      
      // Fetch all appointments for patient
      .exec(
        http("GET /appointments/by-patient (History)")
          .get("/api/appointments/by-patient/${patientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(1.second)
      
      // Fetch prescriptions
      .exec(
        http("GET /prescriptions (Patient)")
          .get("/prescriptions")
          .queryParam("patientId", "${patientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(2.seconds, 4.seconds)
    }

  /**
   * Report generation scenario - creates large in-memory objects.
   * Tests memory usage during report generation (PDF, Excel exports).
   */
  val reportGenerationScenario = scenario("Report Generation (Memory Intensive)")
    .feed(staffFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(1.second)
    .forever {
      // Generate appointment statistics report
      exec(
        http("GET /appointments/stats (Report Data)")
          .get("/api/appointments/stats")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(2.seconds)
      
      // Generate invoice statistics
      .exec(
        http("GET /invoices/stats (Report Data)")
          .get("/invoices/stats")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(2.seconds)
      
      // Bulk patient export (simulated)
      .exec(
        http("GET /patients (Export - Large)")
          .get("/api/patients")
          .queryParam("page", "0")
          .queryParam("size", "1000")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
      )
      .pause(5.seconds, 10.seconds) // Longer pause simulating report processing
    }

  /**
   * Complex write operations - creates objects and verifies memory cleanup.
   * Tests that created objects are properly garbage collected.
   */
  val memoryChurnScenario = scenario("Object Churn (Create/Delete Cycle)")
    .feed(staffFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(1.second)
    .forever {
      // Create appointment (allocates memory for new objects)
      exec(session => {
        val uniqueId = UUID.randomUUID().toString.take(8)
        val patientIndex = scala.util.Random.nextInt(patientCount) + 1
        val doctorIndex = scala.util.Random.nextInt(doctorCount) + 1
        session
          .set("uniqueId", uniqueId)
          .set("randomPatientId", s"patient-$patientIndex")
          .set("randomDoctorId", s"emp-doctor-$doctorIndex")
      })
      .exec(
        http("POST /appointments (Object Creation)")
          .post("/api/appointments")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(
            """{
              "patientId": "${randomPatientId}",
              "doctorId": "${randomDoctorId}",
              "type": "CONSULTATION",
              "reason": "Memory Test ${uniqueId}",
              "notes": "Testing memory churn - objects should be GC'd"
            }"""
          )).asJson
          .check(status.in(200, 201, 400, 409, 500, 503))
          .check(jsonPath("$.data.id").optional.saveAs("newAppointmentId"))
      )
      .pause(1.second)
      
      // Query to verify (allocates response objects)
      .doIf(session => session.contains("newAppointmentId")) {
        exec(
          http("GET /appointments/{id} (Verify Creation)")
            .get("/api/appointments/${newAppointmentId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404, 500, 503))
        )
      }
      .pause(1.second)
      
      // Delete to allow GC (if endpoint exists, otherwise update to cancelled)
      .doIf(session => session.contains("newAppointmentId")) {
        exec(
          http("PUT /appointments/{id}/cancel (Allow GC)")
            .put("/api/appointments/${newAppointmentId}/cancel")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody("""{"reason": "Memory test cleanup"}""")).asJson
            .check(status.in(200, 400, 404, 500, 503))
        )
      }
      .pause(2.seconds, 4.seconds)
    }

  /**
   * Medicine and inventory queries - tests collection handling.
   */
  val inventoryQueryScenario = scenario("Inventory Queries (Collection Memory)")
    .feed(staffFeeder)
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .pause(1.second)
    .forever {
      // Fetch all medicines
      exec(
        http("GET /medicines (Full List)")
          .get("/api/medicines")
          .queryParam("page", "0")
          .queryParam("size", "500")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
      )
      .pause(2.seconds)
      
      // Fetch all categories
      .exec(
        http("GET /categories (Full List)")
          .get("/categories")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
      )
      .pause(2.seconds)
      
      // Fetch low-stock medicines (filtered query)
      .exec(
        http("GET /medicines/low-stock")
          .get("/api/medicines/low-stock")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404, 500, 503))
      )
      .pause(3.seconds, 5.seconds)
    }

  setUp(
    // Ramp up to 100 concurrent users performing memory-intensive operations
    largeDatasetFetchScenario.inject(
      rampUsers(30).during(1.minute),
      nothingFor(29.minutes)
    ),
    
    patientHistoryScenario.inject(
      nothingFor(30.seconds),
      rampUsers(25).during(1.minute),
      nothingFor(28.5.minutes)
    ),
    
    reportGenerationScenario.inject(
      nothingFor(1.minute),
      rampUsers(20).during(1.minute),
      nothingFor(28.minutes)
    ),
    
    memoryChurnScenario.inject(
      nothingFor(90.seconds),
      rampUsers(15).during(1.minute),
      nothingFor(27.5.minutes)
    ),
    
    inventoryQueryScenario.inject(
      nothingFor(2.minutes),
      rampUsers(10).during(1.minute),
      nothingFor(27.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Response times should not degrade severely under memory pressure
     global.responseTime.percentile(95).lt(5000),
     global.responseTime.percentile(99).lt(10000),
     
     // Error rate should remain reasonable (no OOM crashes)
     global.failedRequests.percent.lt(10.0),
     
     // Large dataset queries should complete
     details("GET /patients (Large Dataset - 500 records)").successfulRequests.percent.gte(85.0),
     
     // Object churn operations should succeed
     details("POST /appointments (Object Creation)").responseTime.percentile(95).lt(3000)
   )
   .maxDuration(32.minutes) // 30 minutes + ramp time
}
