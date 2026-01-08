package com.hms.api_gateway.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-HR: HR and Schedule Management Flow End-to-End Tests
 * 
 * Tests complete HR management workflows through the API Gateway:
 * 1. Admin creates department
 * 2. Admin creates employee/doctor
 * 3. Admin creates schedule for doctor
 * 4. View doctor availability
 * 
 * This flow covers the complete hospital staff and schedule management process.
 */
@DisplayName("E2E-HR: HR and Schedule Management Flow")
class HRManagementFlowE2ETest extends E2ETestBase {

    private String adminToken;
    private String doctorToken;
    private String patientToken;
    
    private String testDepartmentId;
    private String testDoctorAccountId;
    private String testDoctorEmployeeId;

    @BeforeEach
    void setUpTestUsers() {
        // Create ADMIN user for HR management operations
        adminToken = registerAndLogin(
            generateUniqueEmail("admin"),
            "AdminPass123!",
            "ADMIN"
        );

        // Create DOCTOR user for verification
        String doctorEmail = generateUniqueEmail("doctor");
        doctorToken = registerAndLogin(doctorEmail, "DoctorPass123!", "DOCTOR");
        
        // Extract doctor's account ID from login response for linking employee record
        Response loginResponse = given()
            .body(Map.of("email", doctorEmail, "password", "DoctorPass123!"))
            .post("/auth/login");
        testDoctorAccountId = loginResponse.jsonPath().getString("data.id");

        // Create PATIENT user for viewing availability
        patientToken = registerAndLogin(
            generateUniqueEmail("patient"),
            "PatientPass123!",
            "PATIENT"
        );
    }

    @Nested
    @DisplayName("E2E-HR-001: Admin creates department")
    class DepartmentCreationTest {

        @Test
        @DisplayName("Should successfully create a new department with valid data")
        void shouldCreateDepartmentSuccessfully() {
            // Given: Valid department data
            Map<String, Object> departmentRequest = new HashMap<>();
            departmentRequest.put("name", "Cardiology-" + System.currentTimeMillis());
            departmentRequest.put("description", "Department specializing in heart and cardiovascular diseases");
            departmentRequest.put("location", "Building A, Floor 3");
            departmentRequest.put("phoneExtension", "3301");
            departmentRequest.put("status", "ACTIVE");

            // When: Admin creates a department
            Response response = givenAuth(adminToken)
                .body(departmentRequest)
                .post("/hr/departments");

            // Then: Department is created successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.name", equalTo(departmentRequest.get("name")))
                .body("data.description", equalTo(departmentRequest.get("description")))
                .body("data.location", equalTo(departmentRequest.get("location")))
                .body("data.phoneExtension", equalTo(departmentRequest.get("phoneExtension")))
                .body("data.status", equalTo("ACTIVE"));

            // Save department ID for subsequent tests
            testDepartmentId = response.jsonPath().getString("data.id");
        }

        @Test
        @DisplayName("Should reject department creation with missing required fields")
        void shouldRejectDepartmentWithMissingFields() {
            // Given: Department data missing required name field
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("description", "Test department");
            invalidRequest.put("location", "Building B");
            invalidRequest.put("phoneExtension", "1234");
            invalidRequest.put("status", "ACTIVE");

            // When: Admin attempts to create department
            Response response = givenAuth(adminToken)
                .body(invalidRequest)
                .post("/hr/departments");

            // Then: Request is rejected with validation error
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should reject department creation without admin authorization")
        void shouldRejectDepartmentCreationByNonAdmin() {
            // Given: Valid department data but non-admin user
            Map<String, Object> departmentRequest = new HashMap<>();
            departmentRequest.put("name", "Unauthorized Dept");
            departmentRequest.put("description", "Should not be created");
            departmentRequest.put("location", "Building X");
            departmentRequest.put("phoneExtension", "9999");
            departmentRequest.put("status", "ACTIVE");

            // When: Doctor (non-admin) attempts to create department
            Response response = givenAuth(doctorToken)
                .body(departmentRequest)
                .post("/hr/departments");

            // Then: Request is rejected with authorization error
            response.then()
                .statusCode(anyOf(equalTo(403), equalTo(401)));
        }

        @Test
        @DisplayName("Should successfully list all departments")
        void shouldListDepartments() {
            // Given: At least one department exists (created in previous test)
            Map<String, Object> departmentRequest = new HashMap<>();
            departmentRequest.put("name", "Neurology-" + System.currentTimeMillis());
            departmentRequest.put("description", "Brain and nervous system department");
            departmentRequest.put("location", "Building B, Floor 2");
            departmentRequest.put("phoneExtension", "2201");
            departmentRequest.put("status", "ACTIVE");

            givenAuth(adminToken)
                .body(departmentRequest)
                .post("/hr/departments");

            // When: Admin retrieves list of departments
            Response response = givenAuth(adminToken)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/departments/all");

            // Then: Departments are returned successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue())
                .body("data.content", hasSize(greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("Should successfully retrieve department by ID")
        void shouldGetDepartmentById() {
            // Given: A created department
            Map<String, Object> departmentRequest = new HashMap<>();
            String deptName = "Pediatrics-" + System.currentTimeMillis();
            departmentRequest.put("name", deptName);
            departmentRequest.put("description", "Children's healthcare department");
            departmentRequest.put("location", "Building C, Floor 1");
            departmentRequest.put("phoneExtension", "1101");
            departmentRequest.put("status", "ACTIVE");

            Response createResponse = givenAuth(adminToken)
                .body(departmentRequest)
                .post("/hr/departments");
            String departmentId = createResponse.jsonPath().getString("data.id");

            // When: Admin retrieves the department by ID
            Response response = givenAuth(adminToken)
                .get("/hr/departments/" + departmentId);

            // Then: Department details are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(departmentId))
                .body("data.name", equalTo(deptName))
                .body("data.location", equalTo("Building C, Floor 1"));
        }
    }

    @Nested
    @DisplayName("E2E-HR-002: Admin creates employee/doctor")
    class EmployeeCreationTest {

        @BeforeEach
        void setUpDepartment() {
            // Create a department for employee assignment
            if (testDepartmentId == null) {
                Map<String, Object> departmentRequest = new HashMap<>();
                departmentRequest.put("name", "General Medicine-" + System.currentTimeMillis());
                departmentRequest.put("description", "General medical services");
                departmentRequest.put("location", "Building A, Floor 1");
                departmentRequest.put("phoneExtension", "1001");
                departmentRequest.put("status", "ACTIVE");

                Response response = givenAuth(adminToken)
                    .body(departmentRequest)
                    .post("/hr/departments");
                testDepartmentId = response.jsonPath().getString("data.id");
            }
        }

        @Test
        @DisplayName("Should successfully create a doctor employee with valid data")
        void shouldCreateDoctorEmployee() {
            // Given: Valid doctor employee data
            Map<String, Object> employeeRequest = new HashMap<>();
            employeeRequest.put("accountId", testDoctorAccountId);
            employeeRequest.put("fullName", "Dr. John Smith");
            employeeRequest.put("role", "DOCTOR");
            employeeRequest.put("departmentId", testDepartmentId);
            employeeRequest.put("specialization", "Cardiology");
            employeeRequest.put("licenseNumber", "MD-12345");
            employeeRequest.put("phoneNumber", "1234567890");
            employeeRequest.put("address", "123 Medical Plaza, City");
            employeeRequest.put("status", "ACTIVE");

            // When: Admin creates a doctor employee
            Response response = givenAuth(adminToken)
                .body(employeeRequest)
                .post("/hr/employees");

            // Then: Employee is created successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.accountId", equalTo(testDoctorAccountId))
                .body("data.fullName", equalTo("Dr. John Smith"))
                .body("data.role", equalTo("DOCTOR"))
                .body("data.specialization", equalTo("Cardiology"))
                .body("data.licenseNumber", equalTo("MD-12345"))
                .body("data.status", equalTo("ACTIVE"));

            // Save employee ID for subsequent tests
            testDoctorEmployeeId = response.jsonPath().getString("data.id");
        }

        @Test
        @DisplayName("Should successfully create a nurse employee")
        void shouldCreateNurseEmployee() {
            // Given: Valid nurse employee data
            Map<String, Object> employeeRequest = new HashMap<>();
            employeeRequest.put("fullName", "Jane Doe RN");
            employeeRequest.put("role", "NURSE");
            employeeRequest.put("departmentId", testDepartmentId);
            employeeRequest.put("licenseNumber", "RN-54321");
            employeeRequest.put("phoneNumber", "9876543210");
            employeeRequest.put("address", "456 Healthcare Ave");
            employeeRequest.put("status", "ACTIVE");

            // When: Admin creates a nurse employee
            Response response = givenAuth(adminToken)
                .body(employeeRequest)
                .post("/hr/employees");

            // Then: Employee is created successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.fullName", equalTo("Jane Doe RN"))
                .body("data.role", equalTo("NURSE"));
        }

        @Test
        @DisplayName("Should reject employee creation with invalid license number format")
        void shouldRejectInvalidLicenseNumber() {
            // Given: Employee data with invalid license number format
            Map<String, Object> employeeRequest = new HashMap<>();
            employeeRequest.put("fullName", "Invalid Doctor");
            employeeRequest.put("role", "DOCTOR");
            employeeRequest.put("departmentId", testDepartmentId);
            employeeRequest.put("licenseNumber", "INVALID123"); // Should be XX-12345 format
            employeeRequest.put("phoneNumber", "1234567890");
            employeeRequest.put("status", "ACTIVE");

            // When: Admin attempts to create employee
            Response response = givenAuth(adminToken)
                .body(employeeRequest)
                .post("/hr/employees");

            // Then: Request is rejected with validation error
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should successfully list all employees")
        void shouldListEmployees() {
            // Given: At least one employee exists
            // When: Admin retrieves list of employees
            Response response = givenAuth(adminToken)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/employees/all");

            // Then: Employees are returned successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }

        @Test
        @DisplayName("Should successfully get employee by ID")
        void shouldGetEmployeeById() {
            // Given: A created employee
            Map<String, Object> employeeRequest = new HashMap<>();
            employeeRequest.put("fullName", "Test Employee");
            employeeRequest.put("role", "ADMIN_STAFF");
            employeeRequest.put("departmentId", testDepartmentId);
            employeeRequest.put("phoneNumber", "5551234567");
            employeeRequest.put("status", "ACTIVE");

            Response createResponse = givenAuth(adminToken)
                .body(employeeRequest)
                .post("/hr/employees");
            String employeeId = createResponse.jsonPath().getString("data.id");

            // When: Admin retrieves employee by ID
            Response response = givenAuth(adminToken)
                .get("/hr/employees/" + employeeId);

            // Then: Employee details are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(employeeId))
                .body("data.fullName", equalTo("Test Employee"));
        }

        @Test
        @DisplayName("Should allow doctor to view their own employee profile")
        void shouldAllowDoctorToViewOwnProfile() {
            // Given: Doctor employee is created and linked to account
            Map<String, Object> employeeRequest = new HashMap<>();
            employeeRequest.put("accountId", testDoctorAccountId);
            employeeRequest.put("fullName", "Dr. Self Profile");
            employeeRequest.put("role", "DOCTOR");
            employeeRequest.put("departmentId", testDepartmentId);
            employeeRequest.put("specialization", "Internal Medicine");
            employeeRequest.put("licenseNumber", "MD-99999");
            employeeRequest.put("phoneNumber", "5559999999");
            employeeRequest.put("status", "ACTIVE");

            givenAuth(adminToken)
                .body(employeeRequest)
                .post("/hr/employees");

            // When: Doctor views their own profile
            Response response = givenAuth(doctorToken)
                .get("/hr/employees/me");

            // Then: Doctor can see their employee profile
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.accountId", equalTo(testDoctorAccountId))
                .body("data.fullName", equalTo("Dr. Self Profile"));
        }
    }

    @Nested
    @DisplayName("E2E-HR-003: Admin creates schedule for doctor")
    class ScheduleCreationTest {

        @BeforeEach
        void setUpDoctorEmployee() {
            // Create department and doctor employee if not already created
            if (testDepartmentId == null) {
                Map<String, Object> departmentRequest = new HashMap<>();
                departmentRequest.put("name", "Surgery-" + System.currentTimeMillis());
                departmentRequest.put("description", "Surgical department");
                departmentRequest.put("location", "Building B, Floor 5");
                departmentRequest.put("phoneExtension", "5001");
                departmentRequest.put("status", "ACTIVE");

                Response response = givenAuth(adminToken)
                    .body(departmentRequest)
                    .post("/hr/departments");
                testDepartmentId = response.jsonPath().getString("data.id");
            }

            if (testDoctorEmployeeId == null) {
                Map<String, Object> employeeRequest = new HashMap<>();
                employeeRequest.put("accountId", testDoctorAccountId);
                employeeRequest.put("fullName", "Dr. Schedule Test");
                employeeRequest.put("role", "DOCTOR");
                employeeRequest.put("departmentId", testDepartmentId);
                employeeRequest.put("specialization", "General Surgery");
                employeeRequest.put("licenseNumber", "SG-11111");
                employeeRequest.put("phoneNumber", "5551111111");
                employeeRequest.put("status", "ACTIVE");

                Response response = givenAuth(adminToken)
                    .body(employeeRequest)
                    .post("/hr/employees");
                testDoctorEmployeeId = response.jsonPath().getString("data.id");
            }
        }

        @Test
        @DisplayName("Should successfully create a doctor schedule")
        void shouldCreateDoctorSchedule() {
            // Given: Valid schedule data for future date
            LocalDate futureDate = LocalDate.now().plusDays(7);
            Map<String, Object> scheduleRequest = new HashMap<>();
            scheduleRequest.put("employeeId", testDoctorEmployeeId);
            scheduleRequest.put("workDate", futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            scheduleRequest.put("startTime", "08:00:00");
            scheduleRequest.put("endTime", "17:00:00");
            scheduleRequest.put("status", "AVAILABLE");
            scheduleRequest.put("notes", "Regular weekday shift");

            // When: Admin creates a schedule
            Response response = givenAuth(adminToken)
                .body(scheduleRequest)
                .post("/hr/schedules");

            // Then: Schedule is created successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.workDate", equalTo(futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .body("data.startTime", notNullValue())
                .body("data.endTime", notNullValue())
                .body("data.status", anyOf(equalTo("AVAILABLE"), equalTo("SCHEDULED")))
                .body("data.notes", equalTo("Regular weekday shift"));
        }

        @Test
        @DisplayName("Should successfully create multiple schedules for a week")
        void shouldCreateWeeklySchedules() {
            // Given: Schedule data for 5 working days
            LocalDate startDate = LocalDate.now().plusDays(10);
            
            for (int i = 0; i < 5; i++) {
                LocalDate workDate = startDate.plusDays(i);
                Map<String, Object> scheduleRequest = new HashMap<>();
                scheduleRequest.put("employeeId", testDoctorEmployeeId);
                scheduleRequest.put("workDate", workDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                scheduleRequest.put("startTime", "09:00:00");
                scheduleRequest.put("endTime", "18:00:00");
                scheduleRequest.put("status", "AVAILABLE");
                scheduleRequest.put("notes", "Day " + (i + 1) + " of work week");

                // When: Admin creates each schedule
                Response response = givenAuth(adminToken)
                    .body(scheduleRequest)
                    .post("/hr/schedules");

                // Then: Each schedule is created successfully
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.workDate", equalTo(workDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            }

            // Verify all schedules were created
            Response listResponse = givenAuth(adminToken)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/schedules/all");

            listResponse.then()
                .statusCode(200)
                .body("data.content", hasSize(greaterThanOrEqualTo(5)));
        }

        @Test
        @DisplayName("Should reject schedule creation with missing required fields")
        void shouldRejectScheduleWithMissingFields() {
            // Given: Schedule data missing required workDate field
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("employeeId", testDoctorEmployeeId);
            invalidRequest.put("startTime", "08:00:00");
            invalidRequest.put("endTime", "17:00:00");
            invalidRequest.put("status", "AVAILABLE");

            // When: Admin attempts to create schedule
            Response response = givenAuth(adminToken)
                .body(invalidRequest)
                .post("/hr/schedules");

            // Then: Request is rejected with validation error
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should successfully update schedule status")
        void shouldUpdateScheduleStatus() {
            // Given: An existing schedule
            LocalDate futureDate = LocalDate.now().plusDays(15);
            Map<String, Object> scheduleRequest = new HashMap<>();
            scheduleRequest.put("employeeId", testDoctorEmployeeId);
            scheduleRequest.put("workDate", futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            scheduleRequest.put("startTime", "08:00:00");
            scheduleRequest.put("endTime", "16:00:00");
            scheduleRequest.put("status", "AVAILABLE");

            Response createResponse = givenAuth(adminToken)
                .body(scheduleRequest)
                .post("/hr/schedules");
            String scheduleId = createResponse.jsonPath().getString("data.id");

            // When: Admin updates the schedule
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("employeeId", testDoctorEmployeeId);
            updateRequest.put("workDate", futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            updateRequest.put("startTime", "10:00:00"); // Changed time
            updateRequest.put("endTime", "18:00:00");   // Changed time
            updateRequest.put("status", "AVAILABLE");
            updateRequest.put("notes", "Updated schedule");

            Response response = givenAuth(adminToken)
                .body(updateRequest)
                .put("/hr/schedules/" + scheduleId);

            // Then: Schedule is updated successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true));
        }

        @Test
        @DisplayName("Should successfully retrieve schedule by ID")
        void shouldGetScheduleById() {
            // Given: A created schedule
            LocalDate futureDate = LocalDate.now().plusDays(20);
            Map<String, Object> scheduleRequest = new HashMap<>();
            scheduleRequest.put("employeeId", testDoctorEmployeeId);
            scheduleRequest.put("workDate", futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            scheduleRequest.put("startTime", "08:00:00");
            scheduleRequest.put("endTime", "17:00:00");
            scheduleRequest.put("status", "AVAILABLE");

            Response createResponse = givenAuth(adminToken)
                .body(scheduleRequest)
                .post("/hr/schedules");
            String scheduleId = createResponse.jsonPath().getString("data.id");

            // When: Admin retrieves schedule by ID
            Response response = givenAuth(adminToken)
                .get("/hr/schedules/" + scheduleId);

            // Then: Schedule details are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(scheduleId))
                .body("data.workDate", equalTo(futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
    }

    @Nested
    @DisplayName("E2E-HR-004: View doctor availability")
    class DoctorAvailabilityTest {

        @BeforeEach
        void setUpDoctorSchedules() {
            // Create department, doctor employee, and schedules if not already created
            if (testDepartmentId == null) {
                Map<String, Object> departmentRequest = new HashMap<>();
                departmentRequest.put("name", "Emergency-" + System.currentTimeMillis());
                departmentRequest.put("description", "Emergency department");
                departmentRequest.put("location", "Building A, Ground Floor");
                departmentRequest.put("phoneExtension", "0001");
                departmentRequest.put("status", "ACTIVE");

                Response response = givenAuth(adminToken)
                    .body(departmentRequest)
                    .post("/hr/departments");
                testDepartmentId = response.jsonPath().getString("data.id");
            }

            if (testDoctorEmployeeId == null) {
                Map<String, Object> employeeRequest = new HashMap<>();
                employeeRequest.put("accountId", testDoctorAccountId);
                employeeRequest.put("fullName", "Dr. Available");
                employeeRequest.put("role", "DOCTOR");
                employeeRequest.put("departmentId", testDepartmentId);
                employeeRequest.put("specialization", "Emergency Medicine");
                employeeRequest.put("licenseNumber", "EM-22222");
                employeeRequest.put("phoneNumber", "5552222222");
                employeeRequest.put("status", "ACTIVE");

                Response response = givenAuth(adminToken)
                    .body(employeeRequest)
                    .post("/hr/employees");
                testDoctorEmployeeId = response.jsonPath().getString("data.id");

                // Create some schedules for the doctor
                LocalDate startDate = LocalDate.now().plusDays(1);
                for (int i = 0; i < 3; i++) {
                    LocalDate workDate = startDate.plusDays(i);
                    Map<String, Object> scheduleRequest = new HashMap<>();
                    scheduleRequest.put("employeeId", testDoctorEmployeeId);
                    scheduleRequest.put("workDate", workDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    scheduleRequest.put("startTime", "08:00:00");
                    scheduleRequest.put("endTime", "16:00:00");
                    scheduleRequest.put("status", "AVAILABLE");

                    givenAuth(adminToken)
                        .body(scheduleRequest)
                        .post("/hr/schedules");
                }
            }
        }

        @Test
        @DisplayName("Should allow patient to view doctor schedules")
        void shouldViewDoctorSchedules() {
            // Given: Patient wants to find available doctors
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);

            // When: Patient queries doctor schedules
            Response response = givenAuth(patientToken)
                .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/schedules/doctors");

            // Then: Available doctor schedules are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }

        @Test
        @DisplayName("Should filter doctor schedules by status")
        void shouldFilterSchedulesByStatus() {
            // Given: Patient wants only available schedules
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(10);

            // When: Patient queries with status filter
            Response response = givenAuth(patientToken)
                .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("status", "AVAILABLE")
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/schedules/doctors");

            // Then: Only available schedules are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }

        @Test
        @DisplayName("Should filter doctor schedules by specific doctor")
        void shouldFilterSchedulesByDoctor() {
            // Given: Patient wants schedules for specific doctor
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(10);

            // When: Patient queries with doctor ID filter
            Response response = givenAuth(patientToken)
                .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("doctorId", testDoctorEmployeeId)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/schedules/doctors");

            // Then: Only schedules for that doctor are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }

        @Test
        @DisplayName("Should filter doctor schedules by department")
        void shouldFilterSchedulesByDepartment() {
            // Given: Patient wants schedules for specific department
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(10);

            // When: Patient queries with department ID filter
            Response response = givenAuth(patientToken)
                .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("departmentId", testDepartmentId)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/schedules/doctors");

            // Then: Only schedules for that department are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }

        @Test
        @DisplayName("Should get schedule by doctor and date")
        void shouldGetScheduleByDoctorAndDate() {
            // Given: A specific doctor and date
            LocalDate targetDate = LocalDate.now().plusDays(1);

            // When: Checking if doctor has schedule on specific date
            Response response = givenAuth(patientToken)
                .queryParam("doctorId", testDoctorEmployeeId)
                .queryParam("date", targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .get("/hr/schedules/by-doctor-date");

            // Then: Schedule for that doctor on that date is returned (if exists)
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
            
            if (response.statusCode() == 200) {
                response.then()
                    .body("success", equalTo(true))
                    .body("data.workDate", equalTo(targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            }
        }

        @Test
        @DisplayName("Should allow doctor to view their own schedules")
        void shouldAllowDoctorToViewOwnSchedules() {
            // Given: Doctor wants to check their own schedule
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(14);

            // When: Doctor queries their schedules using /me endpoint
            Response response = givenAuth(doctorToken)
                .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .get("/hr/schedules/me");

            // Then: Doctor's schedules are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue());
        }

        @Test
        @DisplayName("Should return empty list when no schedules exist in date range")
        void shouldReturnEmptyListWhenNoSchedules() {
            // Given: Date range far in the future with no schedules
            LocalDate startDate = LocalDate.now().plusYears(1);
            LocalDate endDate = LocalDate.now().plusYears(1).plusDays(7);

            // When: Patient queries for schedules
            Response response = givenAuth(patientToken)
                .queryParam("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/hr/schedules/doctors");

            // Then: Empty list is returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", anyOf(nullValue(), hasSize(0)));
        }
    }
}
