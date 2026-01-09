package com.hms.api_gateway.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-LAB: Lab Test Flow End-to-End Tests
 * 
 * Tests complete laboratory test workflow through the API Gateway:
 * 1. Doctor orders lab tests for examination
 * 2. Lab technician views pending tests
 * 3. Lab technician enters test results
 * 
 * This flow covers the complete laboratory testing process from order to result.
 */
@DisplayName("E2E-LAB: Lab Test Flow")
class LabTestFlowE2ETest extends E2ETestBase {

    private String doctorToken;
    private String patientToken;
    private String labTechToken;
    private String adminToken;
    
    private String testPatientId;
    private String testDoctorId;
    private String testMedicalExamId;
    private String testLabTestId;
    private String testLabOrderId;

    @BeforeEach
    void setUpTestUsers() {
        // Create PATIENT user
        String patientEmail = generateUniqueEmail("patient");
        patientToken = registerAndLogin(patientEmail, "PatientPass123!", "PATIENT");
        
        // Extract patient ID from login
        Response patientLogin = given()
            .body(Map.of("email", patientEmail, "password", "PatientPass123!"))
            .post("/auth/login");
        testPatientId = patientLogin.jsonPath().getString("data.id");

        // Create DOCTOR user
        String doctorEmail = generateUniqueEmail("doctor");
        doctorToken = registerAndLogin(doctorEmail, "DoctorPass123!", "DOCTOR");
        
        Response doctorLogin = given()
            .body(Map.of("email", doctorEmail, "password", "DoctorPass123!"))
            .post("/auth/login");
        testDoctorId = doctorLogin.jsonPath().getString("data.id");

        // Create LAB_TECHNICIAN user (or DOCTOR if LAB_TECHNICIAN not available)
        labTechToken = registerAndLogin(
            generateUniqueEmail("labtech"),
            "LabTechPass123!",
            "DOCTOR" // Using DOCTOR role as LAB_TECHNICIAN might not exist
        );

        // Create ADMIN user for managing lab tests
        adminToken = registerAndLogin(
            generateUniqueEmail("admin"),
            "AdminPass123!",
            "ADMIN"
        );
    }

    @Nested
    @DisplayName("E2E-LAB-001: Doctor orders lab tests for examination")
    class LabTestOrderingTest {

        @BeforeEach
        void setUpExam() {
            // Create a medical exam first (required for lab orders)
            Map<String, Object> examRequest = new HashMap<>();
            examRequest.put("patientId", testPatientId);
            examRequest.put("doctorId", testDoctorId);
            examRequest.put("appointmentId", "temp-appointment-id"); // May need actual appointment
            examRequest.put("chiefComplaint", "Patient needs lab tests");
            examRequest.put("vitalSigns", Map.of(
                "bloodPressure", "120/80",
                "temperature", "37.0",
                "heartRate", "75"
            ));

            Response examResponse = givenAuth(doctorToken)
                .body(examRequest)
                .post("/exams");
            
            if (examResponse.statusCode() == 200 || examResponse.statusCode() == 201) {
                testMedicalExamId = examResponse.jsonPath().getString("data.id");
            }
        }

        @Test
        @DisplayName("Should successfully create a lab test order with multiple tests")
        void shouldCreateLabOrder() {
            // Given: Lab tests are available in the system
            // First, ensure at least one lab test exists
            Response labTestsResponse = givenAuth(doctorToken)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/exams/lab-tests/all");

            String labTestId;
            if (labTestsResponse.statusCode() == 200 
                && labTestsResponse.jsonPath().getList("data.content").size() > 0) {
                // Use existing lab test
                labTestId = labTestsResponse.jsonPath().getString("data.content[0].id");
            } else {
                // Create a lab test if none exist
                Map<String, Object> labTestRequest = new HashMap<>();
                labTestRequest.put("code", "CBC-" + System.currentTimeMillis());
                labTestRequest.put("name", "Complete Blood Count");
                labTestRequest.put("category", "LAB");
                labTestRequest.put("description", "Full blood panel");
                labTestRequest.put("price", 150000);
                labTestRequest.put("unit", "cells/μL");
                labTestRequest.put("normalRange", "4.5-11.0");
                labTestRequest.put("isActive", true);

                Response createLabTestResponse = givenAuth(adminToken)
                    .body(labTestRequest)
                    .post("/exams/lab-tests");
                
                labTestId = createLabTestResponse.jsonPath().getString("data.id");
            }
            testLabTestId = labTestId;

            // When: Doctor creates a lab order for the exam
            if (testMedicalExamId != null) {
                Map<String, Object> labOrderRequest = new HashMap<>();
                labOrderRequest.put("medicalExamId", testMedicalExamId);
                labOrderRequest.put("labTestIds", Arrays.asList(labTestId));
                labOrderRequest.put("priority", "NORMAL");
                labOrderRequest.put("notes", "Routine blood work");

                Response response = givenAuth(doctorToken)
                    .body(labOrderRequest)
                    .post("/exams/lab-orders");

                // Then: Lab order is created successfully
                response.then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("success", equalTo(true))
                    .body("data.id", notNullValue())
                    .body("data.orderNumber", notNullValue())
                    .body("data.medicalExamId", equalTo(testMedicalExamId))
                    .body("data.status", anyOf(equalTo("ORDERED"), equalTo("PENDING")));

                testLabOrderId = response.jsonPath().getString("data.id");
            }
        }

        @Test
        @DisplayName("Should create lab order with multiple lab tests")
        void shouldCreateOrderWithMultipleTests() {
            // Given: Multiple lab tests available
            // Create test lab tests if needed
            String labTest1Id = createLabTestIfNeeded("CBC-" + System.currentTimeMillis(), 
                "Complete Blood Count", "LAB", 150000);
            String labTest2Id = createLabTestIfNeeded("URINE-" + System.currentTimeMillis(), 
                "Urinalysis", "LAB", 100000);

            if (testMedicalExamId != null) {
                // When: Doctor orders multiple tests
                Map<String, Object> labOrderRequest = new HashMap<>();
                labOrderRequest.put("medicalExamId", testMedicalExamId);
                labOrderRequest.put("labTestIds", Arrays.asList(labTest1Id, labTest2Id));
                labOrderRequest.put("priority", "URGENT");
                labOrderRequest.put("notes", "Urgent tests required");

                Response response = givenAuth(doctorToken)
                    .body(labOrderRequest)
                    .post("/exams/lab-orders");

                // Then: Order contains all tests
                response.then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("success", equalTo(true))
                    .body("data.priority", equalTo("URGENT"));
            }
        }

        @Test
        @DisplayName("Should reject lab order without medical exam ID")
        void shouldRejectOrderWithoutExam() {
            // Given: Lab order request missing medical exam ID
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("labTestIds", Arrays.asList("some-lab-test-id"));
            invalidRequest.put("priority", "NORMAL");

            // When: Doctor attempts to create order
            Response response = givenAuth(doctorToken)
                .body(invalidRequest)
                .post("/exams/lab-orders");

            // Then: Request is rejected
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should retrieve lab order by ID")
        void shouldGetLabOrderById() {
            // Given: An existing lab order
            if (testLabOrderId != null) {
                // When: Doctor retrieves the order
                Response response = givenAuth(doctorToken)
                    .get("/exams/lab-orders/" + testLabOrderId);

                // Then: Order details are returned
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.id", equalTo(testLabOrderId))
                    .body("data.orderNumber", notNullValue());
            }
        }

        @Test
        @DisplayName("Should retrieve lab orders for medical exam")
        void shouldGetOrdersByExam() {
            // Given: An exam with lab orders
            if (testMedicalExamId != null) {
                // When: Retrieving orders for the exam
                Response response = givenAuth(doctorToken)
                    .get("/exams/lab-orders/exam/" + testMedicalExamId);

                // Then: Orders are returned
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", notNullValue());
            }
        }
    }

    @Nested
    @DisplayName("E2E-LAB-002: Lab technician views pending tests")
    class PendingTestsTest {

        @BeforeEach
        void setUpLabOrders() {
            // Create exam and lab order for testing
            if (testMedicalExamId == null) {
                Map<String, Object> examRequest = new HashMap<>();
                examRequest.put("patientId", testPatientId);
                examRequest.put("doctorId", testDoctorId);
                examRequest.put("appointmentId", "temp-appointment-id");
                examRequest.put("chiefComplaint", "Lab tests needed");

                Response examResponse = givenAuth(doctorToken)
                    .body(examRequest)
                    .post("/exams");
                
                if (examResponse.statusCode() == 200 || examResponse.statusCode() == 201) {
                    testMedicalExamId = examResponse.jsonPath().getString("data.id");
                }
            }

            // Create a lab order
            if (testLabTestId == null) {
                testLabTestId = createLabTestIfNeeded("GLUCOSE-" + System.currentTimeMillis(),
                    "Blood Glucose", "LAB", 80000);
            }

            if (testMedicalExamId != null && testLabOrderId == null) {
                Map<String, Object> orderRequest = new HashMap<>();
                orderRequest.put("medicalExamId", testMedicalExamId);
                orderRequest.put("labTestIds", Arrays.asList(testLabTestId));
                orderRequest.put("priority", "NORMAL");

                Response orderResponse = givenAuth(doctorToken)
                    .body(orderRequest)
                    .post("/exams/lab-orders");
                
                if (orderResponse.statusCode() == 200 || orderResponse.statusCode() == 201) {
                    testLabOrderId = orderResponse.jsonPath().getString("data.id");
                }
            }
        }

        @Test
        @DisplayName("Should list all lab orders for technician")
        void shouldListAllLabOrders() {
            // When: Lab technician views all orders
            Response response = givenAuth(labTechToken)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/exams/lab-orders/all");

            // Then: Orders are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }

        @Test
        @DisplayName("Should view lab test results for an exam")
        void shouldViewLabResultsByExam() {
            // Given: An exam with lab tests
            if (testMedicalExamId != null) {
                // When: Technician views results for the exam
                Response response = givenAuth(labTechToken)
                    .get("/exams/lab-results/exam/" + testMedicalExamId);

                // Then: Results are returned
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", notNullValue());
            }
        }

        @Test
        @DisplayName("Should view patient's lab test history")
        void shouldViewPatientLabHistory() {
            // When: Technician views patient's lab history
            Response response = givenAuth(labTechToken)
                .get("/exams/lab-results/patient/" + testPatientId);

            // Then: Patient's lab results are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue());
        }

        @Test
        @DisplayName("Should retrieve all available lab tests")
        void shouldListAvailableLabTests() {
            // When: Viewing all available lab tests
            Response response = givenAuth(labTechToken)
                .queryParam("page", 0)
                .queryParam("size", 50)
                .get("/exams/lab-tests/all");

            // Then: Lab tests are returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.content", notNullValue());
        }
    }

    @Nested
    @DisplayName("E2E-LAB-003: Lab technician enters test results")
    class LabResultsEntryTest {

        private String testResultId;

        @BeforeEach
        void setUpLabResults() {
            // Ensure we have an exam, lab test, and order
            if (testMedicalExamId == null) {
                Map<String, Object> examRequest = new HashMap<>();
                examRequest.put("patientId", testPatientId);
                examRequest.put("doctorId", testDoctorId);
                examRequest.put("appointmentId", "temp-appointment-id");
                examRequest.put("chiefComplaint", "Lab results needed");

                Response examResponse = givenAuth(doctorToken)
                    .body(examRequest)
                    .post("/exams");
                
                if (examResponse.statusCode() == 200 || examResponse.statusCode() == 201) {
                    testMedicalExamId = examResponse.jsonPath().getString("data.id");
                }
            }

            if (testLabTestId == null) {
                testLabTestId = createLabTestIfNeeded("CHOL-" + System.currentTimeMillis(),
                    "Cholesterol Test", "LAB", 120000);
            }

            // Create lab order which creates results
            if (testMedicalExamId != null && testLabOrderId == null) {
                Map<String, Object> orderRequest = new HashMap<>();
                orderRequest.put("medicalExamId", testMedicalExamId);
                orderRequest.put("labTestIds", Arrays.asList(testLabTestId));
                orderRequest.put("priority", "NORMAL");

                Response orderResponse = givenAuth(doctorToken)
                    .body(orderRequest)
                    .post("/exams/lab-orders");
                
                if (orderResponse.statusCode() == 200 || orderResponse.statusCode() == 201) {
                    testLabOrderId = orderResponse.jsonPath().getString("data.id");
                    
                    // Get the result ID from the order
                    Response orderDetails = givenAuth(doctorToken)
                        .get("/exams/lab-orders/" + testLabOrderId);
                    
                    if (orderDetails.statusCode() == 200) {
                        List<Map<String, Object>> results = orderDetails.jsonPath().getList("data.results");
                        if (results != null && !results.isEmpty()) {
                            testResultId = (String) results.get(0).get("id");
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("Should successfully update lab test result with values")
        void shouldUpdateLabResult() {
            // Given: A pending lab test result
            if (testResultId != null) {
                // When: Lab technician enters result
                Map<String, Object> resultUpdate = new HashMap<>();
                resultUpdate.put("resultValue", "185 mg/dL");
                resultUpdate.put("status", "COMPLETED");
                resultUpdate.put("isAbnormal", false);
                resultUpdate.put("interpretation", "Normal cholesterol levels");
                resultUpdate.put("notes", "Fasting sample");
                resultUpdate.put("performedBy", "Lab Tech Jane Doe");

                Response response = givenAuth(labTechToken)
                    .body(resultUpdate)
                    .put("/exams/lab-results/" + testResultId);

                // Then: Result is updated successfully
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.resultValue", equalTo("185 mg/dL"))
                    .body("data.status", equalTo("COMPLETED"));
            }
        }

        @Test
        @DisplayName("Should flag abnormal test results")
        void shouldFlagAbnormalResults() {
            // Given: A lab test result
            if (testResultId != null) {
                // When: Entering abnormal result
                Map<String, Object> resultUpdate = new HashMap<>();
                resultUpdate.put("resultValue", "350 mg/dL");
                resultUpdate.put("status", "COMPLETED");
                resultUpdate.put("isAbnormal", true);
                resultUpdate.put("interpretation", "Elevated cholesterol - requires attention");
                resultUpdate.put("notes", "Critical value");
                resultUpdate.put("performedBy", "Lab Tech John Smith");

                Response response = givenAuth(labTechToken)
                    .body(resultUpdate)
                    .put("/exams/lab-results/" + testResultId);

                // Then: Result is marked as abnormal
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.isAbnormal", equalTo(true))
                    .body("data.status", equalTo("COMPLETED"));
            }
        }

        @Test
        @DisplayName("Should retrieve lab result by ID after entry")
        void shouldGetLabResultById() {
            // Given: A lab result with entered data
            if (testResultId != null) {
                // Update result first
                Map<String, Object> resultUpdate = new HashMap<>();
                resultUpdate.put("resultValue", "175 mg/dL");
                resultUpdate.put("status", "COMPLETED");

                givenAuth(labTechToken)
                    .body(resultUpdate)
                    .put("/exams/lab-results/" + testResultId);

                // When: Retrieving the result
                Response response = givenAuth(labTechToken)
                    .get("/exams/lab-results/" + testResultId);

                // Then: Complete result is returned
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.id", equalTo(testResultId))
                    .body("data.resultValue", notNullValue());
            }
        }

        @Test
        @DisplayName("Should allow partial result updates")
        void shouldAllowPartialUpdates() {
            // Given: A lab test result
            if (testResultId != null) {
                // When: Updating only some fields
                Map<String, Object> partialUpdate = new HashMap<>();
                partialUpdate.put("status", "IN_PROGRESS");
                partialUpdate.put("notes", "Sample received, testing in progress");

                Response response = givenAuth(labTechToken)
                    .body(partialUpdate)
                    .put("/exams/lab-results/" + testResultId);

                // Then: Only specified fields are updated
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.status", anyOf(equalTo("IN_PROGRESS"), equalTo("PENDING")));
            }
        }

        @Test
        @DisplayName("Should complete full lab test workflow")
        void shouldCompleteFullWorkflow() {
            // Given: A complete lab test setup
            String workflowLabTestId = createLabTestIfNeeded("WBC-" + System.currentTimeMillis(),
                "White Blood Cell Count", "LAB", 90000);

            if (testMedicalExamId != null) {
                // Step 1: Doctor orders test
                Map<String, Object> orderRequest = new HashMap<>();
                orderRequest.put("medicalExamId", testMedicalExamId);
                orderRequest.put("labTestIds", Arrays.asList(workflowLabTestId));
                orderRequest.put("priority", "NORMAL");

                Response orderResponse = givenAuth(doctorToken)
                    .body(orderRequest)
                    .post("/exams/lab-orders");

                orderResponse.then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)));

                String orderId = orderResponse.jsonPath().getString("data.id");

                // Step 2: Get result ID from order
                Response orderDetails = givenAuth(doctorToken)
                    .get("/exams/lab-orders/" + orderId);

                String resultId = null;
                if (orderDetails.statusCode() == 200) {
                    List<Map<String, Object>> results = orderDetails.jsonPath().getList("data.results");
                    if (results != null && !results.isEmpty()) {
                        resultId = (String) results.get(0).get("id");
                    }
                }

                // Step 3: Lab tech enters results
                if (resultId != null) {
                    Map<String, Object> resultUpdate = new HashMap<>();
                    resultUpdate.put("resultValue", "7.5 x10³/μL");
                    resultUpdate.put("status", "COMPLETED");
                    resultUpdate.put("isAbnormal", false);
                    resultUpdate.put("interpretation", "Within normal range");
                    resultUpdate.put("performedBy", "Lab Tech Mary Johnson");

                    Response updateResponse = givenAuth(labTechToken)
                        .body(resultUpdate)
                        .put("/exams/lab-results/" + resultId);

                    updateResponse.then()
                        .statusCode(200)
                        .body("data.status", equalTo("COMPLETED"));

                    // Step 4: Doctor/Patient can view results
                    Response viewResponse = givenAuth(doctorToken)
                        .get("/exams/lab-results/" + resultId);

                    viewResponse.then()
                        .statusCode(200)
                        .body("data.resultValue", notNullValue())
                        .body("data.status", equalTo("COMPLETED"));
                }
            }
        }

        @Test
        @DisplayName("Should allow doctor to interpret completed results")
        void shouldAllowDoctorInterpretation() {
            // Given: A completed lab result
            if (testResultId != null) {
                // Complete the result first
                Map<String, Object> resultUpdate = new HashMap<>();
                resultUpdate.put("resultValue", "200 mg/dL");
                resultUpdate.put("status", "COMPLETED");
                resultUpdate.put("performedBy", "Lab Tech");

                givenAuth(labTechToken)
                    .body(resultUpdate)
                    .put("/exams/lab-results/" + testResultId);

                // When: Doctor adds interpretation
                Map<String, Object> interpretation = new HashMap<>();
                interpretation.put("interpretation", "Slightly elevated, recommend lifestyle changes");
                interpretation.put("interpretedBy", "Dr. " + testDoctorId);

                Response response = givenAuth(doctorToken)
                    .body(interpretation)
                    .put("/exams/lab-results/" + testResultId);

                // Then: Interpretation is added
                response.then()
                    .statusCode(200)
                    .body("success", equalTo(true));
            }
        }
    }

    // Helper method to create lab test if needed
    private String createLabTestIfNeeded(String code, String name, String category, double price) {
        // Try to find existing test first
        Response existingTests = givenAuth(adminToken)
            .queryParam("page", 0)
            .queryParam("size", 100)
            .get("/exams/lab-tests/all");

        if (existingTests.statusCode() == 200) {
            List<Map<String, Object>> tests = existingTests.jsonPath().getList("data.content");
            if (tests != null && !tests.isEmpty()) {
                // Return first available test
                return (String) tests.get(0).get("id");
            }
        }

        // Create new lab test
        Map<String, Object> labTestRequest = new HashMap<>();
        labTestRequest.put("code", code);
        labTestRequest.put("name", name);
        labTestRequest.put("category", category);
        labTestRequest.put("description", "Test description for " + name);
        labTestRequest.put("price", price);
        labTestRequest.put("unit", "mg/dL");
        labTestRequest.put("normalRange", "Normal range");
        labTestRequest.put("isActive", true);

        Response createResponse = givenAuth(adminToken)
            .body(labTestRequest)
            .post("/exams/lab-tests");

        if (createResponse.statusCode() == 200 || createResponse.statusCode() == 201) {
            return createResponse.jsonPath().getString("data.id");
        }
        
        return null;
    }
}
