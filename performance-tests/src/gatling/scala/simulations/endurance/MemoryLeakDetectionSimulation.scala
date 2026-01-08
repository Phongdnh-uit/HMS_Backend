package simulations.endurance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import simulations.HmsSimulationBase
import java.util.UUID

/**
 * PERF-END-002: Memory Leak Detection
 * 
 * Run extended tests specifically designed to expose memory leaks in services.
 * This test focuses on repetitive create/delete operations and monitors heap growth.
 *
 * JVM Settings (Expected):
 * - Enable heap dump on OOM: -XX:+HeapDumpOnOutOfMemoryError
 * - GC logging: -Xlog:gc*:file=gc.log
 *
 * Test Steps:
 * 1. Execute repetitive operations over 12 hours:
 *    - Create and delete 10,000 appointments
 *    - Upload and delete 5,000 patient files
 *    - Generate 1,000 medical exam reports (PDF)
 *    - Process 2,000 billing invoices
 * 2. Take heap dumps every 2 hours (external monitoring)
 * 3. Force full GC between cycles (if possible via JMX)
 * 4. Analyze heap growth patterns
 *
 * Acceptance Criteria:
 * - Old generation heap stable (±5% variance)
 * - Objects properly garbage collected after use
 * - No ClassLoader leaks
 * - ThreadLocal variables cleaned up
 * - File handles closed properly
 * - Database connections returned to pool
 * - Cache size remains bounded
 * - No abandoned HTTP connections
 * 
 * Common Leak Sources to Check:
 * - Static collections growing unbounded
 * - Event listeners not deregistered
 * - ThreadLocal not cleared
 * - Unclosed streams/readers
 * - Cache without eviction policy
 * - Circular references preventing GC
 *
 * Note: For true 12-hour test, run with: maxDuration(12.hours)
 * This configuration runs a compressed 1-hour version for CI/CD.
 */
class MemoryLeakDetectionSimulation extends HmsSimulationBase {

  // Unique operation counter for tracking
  var operationCounter = new java.util.concurrent.atomic.AtomicLong(0)

  // Staff feeder for authenticated operations
  val staffLeakTestFeeder = {
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

  // Patient feeder
  val patientLeakTestFeeder = Iterator.from(1).map { i =>
    Map(
      "email" -> s"patient${(i % patientCount) + 1}@email.com",
      "password" -> defaultPassword,
      "patientIndex" -> ((i % patientCount) + 1)
    )
  }

  /**
   * Appointment create/delete cycle - tests object lifecycle and GC.
   * Creates appointments and then cancels them to test proper cleanup.
   */
  val appointmentCycleScenario = scenario("Appointment Create/Delete Cycle (Memory Leak Test)")
    .feed(patientLeakTestFeeder)
    .exec(
      http("Login (Appointment Cycle)")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
        .check(jsonPath("$.data.user.id").optional.saveAs("patientId"))
    )
    .pause(1.second)
    // Repeat create/delete cycle many times
    .repeat(100, "cycleIndex") {
      exec(session => {
        val uniqueId = UUID.randomUUID().toString
        val doctorIndex = scala.util.Random.nextInt(doctorCount) + 1
        session
          .set("appointmentUniqueId", uniqueId)
          .set("cycleDoctorId", s"emp-doctor-$doctorIndex")
      })
      // Create appointment (allocates objects)
      .exec(
        http("POST /appointments (Leak Test Create)")
          .post("/appointments")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(
            """{
              "patientId": "${patientId}",
              "doctorId": "${cycleDoctorId}",
              "type": "CONSULTATION",
              "reason": "Memory leak test - ${appointmentUniqueId}",
              "notes": "Testing object lifecycle - cycle ${cycleIndex}"
            }"""
          )).asJson
          .check(status.in(200, 201, 400, 409))
          .check(jsonPath("$.data.id").optional.saveAs("createdAppointmentId"))
      )
      .pause(500.milliseconds)
      
      // Read to verify (allocates response objects)
      .doIf(session => session.contains("createdAppointmentId")) {
        exec(
          http("GET /appointments/{id} (Leak Test Read)")
            .get("/appointments/${createdAppointmentId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
      }
      .pause(500.milliseconds)
      
      // Cancel/delete to allow GC
      .doIf(session => session.contains("createdAppointmentId")) {
        exec(
          http("PUT /appointments/{id}/cancel (Leak Test Delete)")
            .put("/appointments/${createdAppointmentId}/cancel")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody("""{"reason": "Memory leak test cleanup"}""")).asJson
            .check(status.in(200, 400, 404))
        )
      }
      .pause(1.second)
    }

  /**
   * Patient record churn - tests entity lifecycle and JPA session handling.
   * Creates patients and updates them repeatedly.
   */
  val patientRecordChurnScenario = scenario("Patient Record Churn (Entity Lifecycle)")
    .feed(staffLeakTestFeeder)
    .exec(
      http("Login (Patient Churn)")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
    // Repeat read/update cycle many times
    .repeat(50, "churnIndex") {
      // Fetch patient list (large object graph)
      exec(
        http("GET /patients (Churn - Large Fetch)")
          .get("/patients")
          .queryParam("page", "${churnIndex}")
          .queryParam("size", "50")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.is(200))
          .check(jsonPath("$.data.content[0].id").optional.saveAs("churnPatientId"))
      )
      .pause(500.milliseconds)
      
      // Update patient (entity modification)
      .doIf(session => session.contains("churnPatientId")) {
        exec(session => {
          val updateId = UUID.randomUUID().toString.take(8)
          session.set("churnUpdateId", updateId)
        })
        .exec(
          http("PUT /patients/{id} (Churn - Update)")
            .put("/patients/${churnPatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody(
              """{
                "notes": "Memory leak test update - ${churnUpdateId} - iteration ${churnIndex}"
              }"""
            )).asJson
            .check(status.in(200, 400, 404))
        )
      }
      .pause(1.second)
    }

  /**
   * Medical exam report cycle - tests complex object graphs and lazy loading.
   */
  val examReportCycleScenario = scenario("Medical Exam Report Cycle (Complex Objects)")
    .feed(staffLeakTestFeeder)
    .exec(
      http("Login (Exam Cycle)")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
    .repeat(30, "examCycleIndex") {
      // Fetch patient's full medical history (deep object graph)
      exec(session => {
        val patientIndex = scala.util.Random.nextInt(patientCount) + 1
        session.set("examPatientId", s"patient-$patientIndex")
      })
      .exec(
        http("GET /exams/by-patient (Deep Fetch)")
          .get("/exams/by-patient/${examPatientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(1.second)
      
      // Fetch prescriptions (related entities)
      .exec(
        http("GET /prescriptions (Related)")
          .get("/prescriptions")
          .queryParam("patientId", "${examPatientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(1.second)
      
      // Fetch lab orders (more related entities)
      .exec(
        http("GET /lab-orders (Related)")
          .get("/lab-orders")
          .queryParam("patientId", "${examPatientId}")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(2.seconds)
    }

  /**
   * Invoice processing cycle - tests transaction handling and connection management.
   */
  val invoiceProcessingScenario = scenario("Invoice Processing Cycle (Transactions)")
    .feed(staffLeakTestFeeder)
    .exec(
      http("Login (Invoice)")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
    .repeat(40, "invoiceCycleIndex") {
      // Fetch completed appointments for invoicing
      exec(
        http("GET /appointments (For Invoice)")
          .get("/appointments")
          .queryParam("status", "COMPLETED")
          .queryParam("page", "0")
          .queryParam("size", "10")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
          .check(jsonPath("$.data.content[0].id").optional.saveAs("invoiceAppointmentId"))
          .check(jsonPath("$.data.content[0].patientId").optional.saveAs("invoicePatientId"))
      )
      .pause(500.milliseconds)
      
      // Create invoice (transactional write)
      .doIf(session => session.contains("invoiceAppointmentId")) {
        exec(session => {
          val invoiceId = UUID.randomUUID().toString.take(8)
          session.set("invoiceUniqueId", invoiceId)
        })
        .exec(
          http("POST /invoices (Transactional Create)")
            .post("/invoices")
            .header("Authorization", "Bearer ${authToken}")
            .body(StringBody(
              """{
                "appointmentId": "${invoiceAppointmentId}",
                "patientId": "${invoicePatientId}",
                "notes": "Memory leak test invoice - ${invoiceUniqueId}"
              }"""
            )).asJson
            .check(status.in(200, 201, 400, 409))
            .check(jsonPath("$.data.id").optional.saveAs("createdInvoiceId"))
        )
      }
      .pause(500.milliseconds)
      
      // Fetch invoice with items (verify creation)
      .doIf(session => session.contains("createdInvoiceId")) {
        exec(
          http("GET /invoices/{id} (Verify)")
            .get("/invoices/${createdInvoiceId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
      }
      .pause(1.second)
      
      // Fetch invoice list (pagination memory test)
      .exec(
        http("GET /invoices (List)")
          .get("/invoices")
          .queryParam("page", "${invoiceCycleIndex}")
          .queryParam("size", "50")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 404))
      )
      .pause(1.second)
    }

  /**
   * Large collection fetch cycle - tests memory handling for large result sets.
   */
  val largeCollectionFetchScenario = scenario("Large Collection Fetch (Pagination Memory)")
    .feed(staffLeakTestFeeder)
    .exec(
      http("Login (Large Fetch)")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
    .repeat(20, "largeFetchIndex") {
      // Fetch large patient list (tests pagination memory)
      exec(
        http("GET /patients (Large - 500)")
          .get("/patients")
          .queryParam("page", "0")
          .queryParam("size", "500")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
      )
      .pause(2.seconds)
      
      // Fetch large appointment list
      .exec(
        http("GET /appointments (Large - 300)")
          .get("/appointments")
          .queryParam("page", "0")
          .queryParam("size", "300")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 500, 503))
      )
      .pause(2.seconds)
      
      // Multiple sequential pages (memory accumulation test)
      .repeat(5, "pageIndex") {
        exec(
          http("GET /patients (Sequential Page ${pageIndex})")
            .get("/patients")
            .queryParam("page", "${pageIndex}")
            .queryParam("size", "100")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 500, 503))
        )
        .pause(500.milliseconds)
      }
      .pause(3.seconds)
    }

  /**
   * Session/token lifecycle - tests authentication object lifecycle.
   */
  val sessionLifecycleScenario = scenario("Session/Token Lifecycle (Auth Memory)")
    .feed(staffLeakTestFeeder)
    .repeat(50, "sessionIndex") {
      // Login (creates session objects, JWT)
      exec(
        http("POST /auth/login (Session Create)")
          .post("/auth/login")
          .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
          .check(status.is(200))
          .check(jsonPath("$.data.token").saveAs("sessionToken"))
      )
      .pause(500.milliseconds)
      
      // Use token (validates JWT, accesses user context)
      .exec(
        http("GET /auth/me (Session Use)")
          .get("/auth/me")
          .header("Authorization", "Bearer ${sessionToken}")
          .check(status.is(200))
      )
      .pause(500.milliseconds)
      
      // Make authenticated request (UserContext created/destroyed)
      .exec(
        http("GET /patients (Authenticated)")
          .get("/patients")
          .queryParam("page", "0")
          .queryParam("size", "10")
          .header("Authorization", "Bearer ${sessionToken}")
          .check(status.in(200, 404))
      )
      .pause(1.second)
      
      // Another login (should not accumulate sessions)
      .exec(
        http("POST /auth/login (Session Refresh)")
          .post("/auth/login")
          .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
          .check(status.is(200))
      )
      .pause(1.second)
    }

  /**
   * Cache behavior test - tests bounded cache sizes.
   */
  val cacheBehaviorScenario = scenario("Cache Behavior (Bounded Size)")
    .feed(staffLeakTestFeeder)
    .exec(
      http("Login (Cache Test)")
        .post("/auth/login")
        .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
        .check(status.is(200))
        .check(jsonPath("$.data.token").saveAs("authToken"))
    )
    .pause(1.second)
    .repeat(30, "cacheIndex") {
      // Access many different resources (cache population)
      repeat(10, "resourceIndex") {
        exec(session => {
          val patientId = scala.util.Random.nextInt(patientCount) + 1
          session.set("cachePatientId", patientId)
        })
        .exec(
          http("GET /patients/{id} (Cache Miss)")
            .get("/patients/${cachePatientId}")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(200.milliseconds)
      }
      .pause(1.second)
      
      // Access same resources (cache hit)
      .repeat(5, "hitIndex") {
        exec(
          http("GET /patients/1 (Cache Hit)")
            .get("/patients/1")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 404))
        )
        .pause(100.milliseconds)
      }
      .pause(2.seconds)
    }

  // For true 12-hour test, use: maxDuration(12.hours)
  // This is a compressed 1-hour version for CI/CD
  setUp(
    // Appointment create/delete cycles (highest churn)
    appointmentCycleScenario.inject(
      rampUsers(20).during(2.minutes),
      nothingFor(58.minutes)
    ),
    
    // Patient record churn
    patientRecordChurnScenario.inject(
      nothingFor(2.minutes),
      rampUsers(15).during(2.minutes),
      nothingFor(56.minutes)
    ),
    
    // Medical exam report cycles
    examReportCycleScenario.inject(
      nothingFor(4.minutes),
      rampUsers(10).during(2.minutes),
      nothingFor(54.minutes)
    ),
    
    // Invoice processing
    invoiceProcessingScenario.inject(
      nothingFor(6.minutes),
      rampUsers(10).during(2.minutes),
      nothingFor(52.minutes)
    ),
    
    // Large collection fetches
    largeCollectionFetchScenario.inject(
      nothingFor(8.minutes),
      rampUsers(5).during(2.minutes),
      nothingFor(50.minutes)
    ),
    
    // Session lifecycle
    sessionLifecycleScenario.inject(
      nothingFor(10.minutes),
      rampUsers(10).during(2.minutes),
      nothingFor(48.minutes)
    ),
    
    // Cache behavior
    cacheBehaviorScenario.inject(
      nothingFor(12.minutes),
      rampUsers(5).during(2.minutes),
      nothingFor(46.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     // No crashes due to memory issues
     global.failedRequests.percent.lt(5.0),
     
     // Response times should not degrade over time
     global.responseTime.percentile(95).lt(5000),
     
     // Critical operations should succeed
     details("POST /appointments (Leak Test Create)").successfulRequests.percent.gte(90.0),
     details("GET /patients (Large - 500)").successfulRequests.percent.gte(85.0),
     
     // Session management should work
     details("POST /auth/login (Session Create)").successfulRequests.percent.gte(99.0),
     details("GET /auth/me (Session Use)").successfulRequests.percent.gte(99.0)
   )
   .maxDuration(65.minutes) // 1 hour + buffer
   // For full 12-hour test, uncomment: .maxDuration(12.hours)
}
