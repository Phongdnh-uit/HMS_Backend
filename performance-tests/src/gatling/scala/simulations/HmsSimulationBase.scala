package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.protocol.HttpProtocolBuilder
import scala.concurrent.duration._

/**
 * Base configuration for all HMS performance simulations.
 * Contains common HTTP protocol settings, feeders, and utility methods.
 */
trait HmsSimulationBase extends Simulation {

  // Configuration from environment variables with defaults
  val baseUrl: String = sys.env.getOrElse("HMS_BASE_URL", "http://localhost:8080")
  val authServiceUrl: String = sys.env.getOrElse("HMS_AUTH_URL", s"$baseUrl")
  
  // Test data counts from seed data
  val patientCount: Int = 1000
  val doctorCount: Int = 60
  val nurseCount: Int = 50
  val receptionistCount: Int = 40
  val adminCount: Int = 5

  // Default password for all test accounts
  val defaultPassword: String = "Password@123"

  // HTTP Protocol Configuration
  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("HMS-Performance-Test/1.0")
    .shareConnections
    // Connection settings for high concurrency
    .maxConnectionsPerHost(50)

  // ==================== FEEDERS ====================

  // Patient credentials feeder (1000 patients)
  val patientFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"patient$i@email.com",
    "password" -> defaultPassword,
    "patientIndex" -> i
  ))

  // Doctor credentials feeder (60 doctors)
  val doctorFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"doctor${(i % doctorCount) + 1}@hms.com",
    "password" -> defaultPassword,
    "doctorIndex" -> ((i % doctorCount) + 1)
  ))

  // Nurse credentials feeder (50 nurses)
  val nurseFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"nurse${(i % nurseCount) + 1}@hms.com",
    "password" -> defaultPassword,
    "nurseIndex" -> ((i % nurseCount) + 1)
  ))

  // Receptionist credentials feeder (40 receptionists)
  val receptionistFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"receptionist${(i % receptionistCount) + 1}@hms.com",
    "password" -> defaultPassword,
    "receptionistIndex" -> ((i % receptionistCount) + 1)
  ))

  // Admin credentials feeder (5 admins)
  val adminFeeder = Iterator.from(1).map(i => Map(
    "email" -> s"admin${(i % adminCount) + 1}@hms.com",
    "password" -> defaultPassword,
    "adminIndex" -> ((i % adminCount) + 1)
  ))

  // Mixed staff feeder (doctors, nurses, receptionists)
  val mixedStaffFeeder = {
    val doctors = (1 to doctorCount).map(i => Map("email" -> s"doctor$i@hms.com", "password" -> defaultPassword, "role" -> "DOCTOR"))
    val nurses = (1 to nurseCount).map(i => Map("email" -> s"nurse$i@hms.com", "password" -> defaultPassword, "role" -> "NURSE"))
    val receptionists = (1 to receptionistCount).map(i => Map("email" -> s"receptionist$i@hms.com", "password" -> defaultPassword, "role" -> "RECEPTIONIST"))
    scala.util.Random.shuffle(doctors ++ nurses ++ receptionists).iterator
  }

  // ==================== COMMON REQUESTS ====================

  // Login request that saves the JWT token
  def login = exec(
    http("Login")
      .post("/api/auth/login")
      .body(StringBody("""{"email": "${email}", "password": "${password}"}""")).asJson
      .check(status.is(200))
      .check(jsonPath("$.data.accessToken").saveAs("authToken"))
      .check(jsonPath("$.data.account.id").optional.saveAs("userId"))
  )

  // Authenticated request helper
  def authHeader = Map("Authorization" -> "Bearer ${authToken}")

  // Get current user profile
  def getCurrentUser = exec(
    http("Get Current User")
      .get("/api/auth/me")
      .headers(authHeader)
      .check(status.is(200))
  )

  // ==================== THINK TIMES ====================
  
  // Standard think times based on user role patterns
  val shortThinkTime = 1.seconds
  val mediumThinkTime = 3.seconds
  val longThinkTime = 5.seconds
  val patientThinkTime = uniformDuration(5.seconds, 10.seconds)
  val doctorThinkTime = uniformDuration(10.seconds, 20.seconds)
  val nurseThinkTime = uniformDuration(5.seconds, 15.seconds)
  val receptionistThinkTime = uniformDuration(3.seconds, 8.seconds)
  val adminThinkTime = uniformDuration(10.seconds, 30.seconds)

  // Random think time helper
  def uniformDuration(min: FiniteDuration, max: FiniteDuration): FiniteDuration = {
    val minMs = min.toMillis
    val maxMs = max.toMillis
    (minMs + scala.util.Random.nextLong(maxMs - minMs)).milliseconds
  }

  // ==================== ASSERTIONS ====================

  // Standard SLA assertions
  def standardAssertions = Seq(
    global.responseTime.percentile(95).lt(500),
    global.responseTime.percentile(99).lt(1000),
    global.successfulRequests.percent.gt(99.0)
  )

  // Strict SLA for query operations
  def queryAssertions = Seq(
    global.responseTime.percentile(95).lt(200),
    global.responseTime.percentile(99).lt(500),
    global.successfulRequests.percent.gt(99.5)
  )

  // Relaxed SLA for write operations
  def writeAssertions = Seq(
    global.responseTime.percentile(95).lt(1500),
    global.responseTime.percentile(99).lt(3000),
    global.successfulRequests.percent.gt(98.0)
  )
}
