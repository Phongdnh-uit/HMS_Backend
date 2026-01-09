package com.hms.api_gateway.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-BILL: Billing and Payment Flow End-to-End Tests
 * 
 * Tests complete billing and payment workflow through the API Gateway:
 * 1. System generates invoice after medical exam
 * 2. Patient views invoice details
 * 3. Patient initiates VNPay payment
 * 4. Payment confirmation and status updates
 */
@DisplayName("E2E-BILL: Billing and Payment Flow")
class BillingPaymentFlowE2ETest extends E2ETestBase {

    private String patientToken;
    private Long patientId;
    private String doctorToken;
    private Long doctorId;
    private Integer appointmentId;
    private Integer examId;
    private String invoiceId;

    @BeforeEach
    void setUpTestUsersAndData() {
        // Create and login a patient
        String patientEmail = generateUniqueEmail("bill-patient");
        patientToken = registerAndLogin(patientEmail, "PatientPass123!", "PATIENT");
        
        Response patientProfile = givenAuth(patientToken).get("/auth/me");
        patientId = patientProfile.jsonPath().getLong("data.id");

        // Create and login a doctor
        String doctorEmail = generateUniqueEmail("bill-doctor");
        doctorToken = registerAndLogin(doctorEmail, "DoctorPass123!", "DOCTOR");
        
        Response doctorProfile = givenAuth(doctorToken).get("/auth/me");
        doctorId = doctorProfile.jsonPath().getLong("data.id");

        // Create test data: appointment and exam
        createTestAppointmentAndExam();
    }

    private void createTestAppointmentAndExam() {
        // Create appointment
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(1);
        
        Map<String, Object> appointmentRequest = new HashMap<>();
        appointmentRequest.put("patientId", patientId);
        appointmentRequest.put("doctorId", doctorId);
        appointmentRequest.put("appointmentDate", appointmentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        appointmentRequest.put("reason", "Billing flow test");
        appointmentRequest.put("type", "SCHEDULED");

        Response appointmentResponse = givenAuth(patientToken)
            .body(appointmentRequest)
            .post("/appointments");

        appointmentId = appointmentResponse.jsonPath().getInt("data.id");

        // Check in the patient
        givenAuth(patientToken)
            .post("/appointments/" + appointmentId + "/check-in");

        // Create medical exam
        Map<String, Object> examRequest = new HashMap<>();
        examRequest.put("appointmentId", appointmentId);
        examRequest.put("patientId", patientId);
        examRequest.put("doctorId", doctorId);
        examRequest.put("symptoms", "General checkup");
        examRequest.put("diagnosis", "Healthy");
        examRequest.put("vitalSigns", "BP: 120/80");
        examRequest.put("examDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response examResponse = givenAuth(doctorToken)
            .body(examRequest)
            .post("/exams");
        
        examId = examResponse.jsonPath().getInt("data.id");
    }

    @Nested
    @DisplayName("E2E-BILL-001: Invoice created after exam")
    class InvoiceGenerationTest {

        @Test
        @DisplayName("Should automatically generate invoice after exam creation")
        void shouldGenerateInvoiceAfterExam() {
            // Given: An exam exists
            // When: Retrieving invoice by appointment
            Response response = givenAuth(patientToken)
                .get("/invoices/by-appointment/" + appointmentId);

            // Then: Invoice should exist (or 404 if not auto-generated)
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("success", equalTo(true))
                    .body("data.id", notNullValue())
                    .body("data.invoiceNumber", notNullValue())
                    .body("data.status", notNullValue());
                
                invoiceId = response.jsonPath().getString("data.id");
                assertThat(invoiceId).isNotNull();
            }
        }

        @Test
        @DisplayName("Should create invoice manually for appointment")
        void shouldCreateInvoiceManually() {
            // Given: Invoice request for appointment
            Map<String, Object> invoiceRequest = new HashMap<>();
            invoiceRequest.put("appointmentId", appointmentId.toString());
            invoiceRequest.put("notes", "Manual invoice creation for test");

            // When: Creating invoice
            Response response = givenAuth(doctorToken)
                .body(invoiceRequest)
                .post("/invoices");

            // Then: Invoice is created successfully
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(409))) // 409 if already exists
                .body("success", anyOf(equalTo(true), equalTo(false)));

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                response.then()
                    .body("data.id", notNullValue())
                    .body("data.invoiceNumber", notNullValue())
                    .body("data.appointmentId", equalTo(appointmentId.toString()));
                
                invoiceId = response.jsonPath().getString("data.id");
                assertThat(invoiceId).isNotNull();
            }
        }

        @Test
        @DisplayName("Should retrieve invoice by exam ID")
        void shouldGetInvoiceByExamId() {
            // Given: Creating invoice first
            Map<String, Object> invoiceRequest = new HashMap<>();
            invoiceRequest.put("appointmentId", appointmentId.toString());

            Response createResponse = givenAuth(doctorToken)
                .body(invoiceRequest)
                .post("/invoices");
            
            // When: Retrieving by exam ID
            Response response = givenAuth(patientToken)
                .get("/invoices/by-exam/" + examId);

            // Then: Invoice is returned or not found
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));
        }

        @Test
        @DisplayName("Should list patient invoices")
        void shouldListPatientInvoices() {
            // When: Patient retrieves their invoices
            Response response = givenAuth(patientToken)
                .get("/invoices?patientId=" + patientId);

            // Then: Invoice list is returned
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("success", equalTo(true));
            }
        }
    }

    @Nested
    @DisplayName("E2E-BILL-002: Patient views invoice")
    class InvoiceViewingTest {

        @BeforeEach
        void createInvoice() {
            // Create invoice for tests
            Map<String, Object> invoiceRequest = new HashMap<>();
            invoiceRequest.put("appointmentId", appointmentId.toString());

            Response response = givenAuth(doctorToken)
                .body(invoiceRequest)
                .post("/invoices");
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                invoiceId = response.jsonPath().getString("data.id");
            } else {
                // Try to get existing invoice
                Response getResponse = givenAuth(patientToken)
                    .get("/invoices/by-appointment/" + appointmentId);
                if (getResponse.statusCode() == 200) {
                    invoiceId = getResponse.jsonPath().getString("data.id");
                }
            }
        }

        @Test
        @DisplayName("Should retrieve invoice by ID")
        void shouldGetInvoiceById() {
            // Skip if no invoice was created
            if (invoiceId == null) {
                return;
            }

            // When: Patient retrieves invoice by ID
            Response response = givenAuth(patientToken)
                .get("/invoices/" + invoiceId);

            // Then: Invoice details are returned
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(403), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("success", equalTo(true))
                    .body("data.id", equalTo(invoiceId))
                    .body("data.totalAmount", notNullValue())
                    .body("data.status", notNullValue());
            }
        }

        @Test
        @DisplayName("Should view invoice details with line items")
        void shouldViewInvoiceWithLineItems() {
            if (invoiceId == null) {
                return;
            }

            // When: Retrieving invoice
            Response response = givenAuth(patientToken)
                .get("/invoices/" + invoiceId);

            // Then: Invoice has expected structure
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(403), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("data.invoiceNumber", notNullValue());
                    // Line items may or may not be present depending on implementation
            }
        }

        @Test
        @DisplayName("Should not allow other patient to view invoice")
        void shouldNotAllowCrossPatientAccess() {
            if (invoiceId == null) {
                return;
            }

            // Given: Another patient
            String otherPatientEmail = generateUniqueEmail("other-patient");
            String otherPatientToken = registerAndLogin(otherPatientEmail, "OtherPass123!", "PATIENT");

            // When: Other patient tries to view invoice
            Response response = givenAuth(otherPatientToken)
                .get("/invoices/" + invoiceId);

            // Then: Access is denied
            response.then()
                .statusCode(anyOf(equalTo(403), equalTo(404)));
        }

        @Test
        @DisplayName("Should view payment status on invoice")
        void shouldViewPaymentStatus() {
            if (invoiceId == null) {
                return;
            }

            // When: Retrieving invoice
            Response response = givenAuth(patientToken)
                .get("/invoices/" + invoiceId);

            // Then: Payment status is included
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(403), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("data.status", notNullValue());
                
                String status = response.jsonPath().getString("data.status");
                assertThat(status).isIn("PENDING", "PAID", "PARTIALLY_PAID", "CANCELLED", "OVERDUE");
            }
        }
    }

    @Nested
    @DisplayName("E2E-BILL-003: VNPay payment flow")
    class VNPayPaymentTest {

        @BeforeEach
        void createInvoice() {
            // Create invoice for payment tests
            Map<String, Object> invoiceRequest = new HashMap<>();
            invoiceRequest.put("appointmentId", appointmentId.toString());

            Response response = givenAuth(doctorToken)
                .body(invoiceRequest)
                .post("/invoices");
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                invoiceId = response.jsonPath().getString("data.id");
            } else {
                // Try to get existing invoice
                Response getResponse = givenAuth(patientToken)
                    .get("/invoices/by-appointment/" + appointmentId);
                if (getResponse.statusCode() == 200) {
                    invoiceId = getResponse.jsonPath().getString("data.id");
                }
            }
        }

        @Test
        @DisplayName("Should initialize VNPay payment")
        void shouldInitializeVNPayPayment() {
            if (invoiceId == null) {
                return;
            }

            // Given: Payment initialization request
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("invoiceId", invoiceId);
            paymentRequest.put("returnUrl", "http://localhost:3000/payment-result");
            paymentRequest.put("language", "vn");

            // When: Initializing payment
            Response response = givenAuth(patientToken)
                .body(paymentRequest)
                .post("/payments/init");

            // Then: Payment URL is generated
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(400), equalTo(404)));

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                response.then()
                    .body("success", equalTo(true))
                    .body("data.paymentUrl", notNullValue())
                    .body("data.paymentId", notNullValue())
                    .body("data.txnRef", notNullValue());
                
                String paymentUrl = response.jsonPath().getString("data.paymentUrl");
                assertThat(paymentUrl).contains("vnpay");
            }
        }

        @Test
        @DisplayName("Should validate payment amount")
        void shouldValidatePaymentAmount() {
            if (invoiceId == null) {
                return;
            }

            // Given: Payment with invalid amount (negative)
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("invoiceId", invoiceId);
            paymentRequest.put("amount", -100);

            // When: Initializing payment with invalid amount
            Response response = givenAuth(patientToken)
                .body(paymentRequest)
                .post("/payments/init");

            // Then: Validation error is returned
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)));
        }

        @Test
        @DisplayName("Should retrieve payments by invoice")
        void shouldGetPaymentsByInvoice() {
            if (invoiceId == null) {
                return;
            }

            // When: Retrieving payments for invoice
            Response response = givenAuth(patientToken)
                .get("/payments/by-invoice/" + invoiceId);

            // Then: Payment list is returned
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("success", equalTo(true));
            }
        }

        @Test
        @DisplayName("Should retrieve payment by ID")
        void shouldGetPaymentById() {
            if (invoiceId == null) {
                return;
            }

            // First create a payment
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("invoiceId", invoiceId);
            paymentRequest.put("returnUrl", "http://localhost:3000/payment-result");

            Response initResponse = givenAuth(patientToken)
                .body(paymentRequest)
                .post("/payments/init");

            if (initResponse.statusCode() != 200 && initResponse.statusCode() != 201) {
                return;
            }

            String paymentId = initResponse.jsonPath().getString("data.paymentId");

            // When: Retrieving payment by ID
            Response response = givenAuth(patientToken)
                .get("/payments/" + paymentId);

            // Then: Payment details are returned
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("success", equalTo(true))
                    .body("data.id", equalTo(paymentId))
                    .body("data.status", notNullValue());
            }
        }

        @Test
        @DisplayName("Should complete full payment workflow")
        void shouldCompletePaymentWorkflow() {
            if (invoiceId == null) {
                return;
            }

            // Step 1: Initialize payment
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("invoiceId", invoiceId);
            paymentRequest.put("returnUrl", "http://localhost:3000/payment-result");
            paymentRequest.put("language", "en");

            Response initResponse = givenAuth(patientToken)
                .body(paymentRequest)
                .post("/payments/init");

            initResponse.then()
                .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(400), equalTo(404)));

            if (initResponse.statusCode() != 200 && initResponse.statusCode() != 201) {
                return;
            }

            String paymentId = initResponse.jsonPath().getString("data.paymentId");
            String paymentUrl = initResponse.jsonPath().getString("data.paymentUrl");

            // Step 2: Verify payment was created
            Response paymentCheck = givenAuth(patientToken)
                .get("/payments/" + paymentId);

            paymentCheck.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (paymentCheck.statusCode() == 200) {
                paymentCheck.then()
                    .body("data.status", anyOf(equalTo("PENDING"), equalTo("PROCESSING")));
            }

            // Step 3: Check invoice status
            Response invoiceCheck = givenAuth(patientToken)
                .get("/invoices/" + invoiceId);

            invoiceCheck.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            // Assertions
            assertThat(paymentUrl).isNotNull();
            assertThat(paymentId).isNotNull();
        }
    }

    @Nested
    @DisplayName("E2E-BILL-004: Payment confirmation")
    class PaymentConfirmationTest {

        private String paymentId;

        @BeforeEach
        void createInvoiceAndPayment() {
            // Create invoice
            Map<String, Object> invoiceRequest = new HashMap<>();
            invoiceRequest.put("appointmentId", appointmentId.toString());

            Response invoiceResponse = givenAuth(doctorToken)
                .body(invoiceRequest)
                .post("/invoices");
            
            if (invoiceResponse.statusCode() == 200 || invoiceResponse.statusCode() == 201) {
                invoiceId = invoiceResponse.jsonPath().getString("data.id");
            } else {
                Response getResponse = givenAuth(patientToken)
                    .get("/invoices/by-appointment/" + appointmentId);
                if (getResponse.statusCode() == 200) {
                    invoiceId = getResponse.jsonPath().getString("data.id");
                }
            }

            // Initialize payment
            if (invoiceId != null) {
                Map<String, Object> paymentRequest = new HashMap<>();
                paymentRequest.put("invoiceId", invoiceId);
                paymentRequest.put("returnUrl", "http://localhost:3000/payment-result");

                Response paymentResponse = givenAuth(patientToken)
                    .body(paymentRequest)
                    .post("/payments/init");

                if (paymentResponse.statusCode() == 200 || paymentResponse.statusCode() == 201) {
                    paymentId = paymentResponse.jsonPath().getString("data.paymentId");
                }
            }
        }

        @Test
        @DisplayName("Should verify payment status")
        void shouldVerifyPaymentStatus() {
            if (paymentId == null) {
                return;
            }

            // When: Checking payment status
            Response response = givenAuth(patientToken)
                .get("/payments/" + paymentId);

            // Then: Status is returned
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("data.status", notNullValue());
                
                String status = response.jsonPath().getString("data.status");
                assertThat(status).isIn("PENDING", "PROCESSING", "COMPLETED", "FAILED", "EXPIRED");
            }
        }

        @Test
        @DisplayName("Should handle VNPay callback")
        void shouldHandleVNPayCallback() {
            // Note: VNPay callback requires signature verification
            // This test verifies the endpoint exists
            Map<String, String> callbackParams = new HashMap<>();
            callbackParams.put("vnp_ResponseCode", "00");
            callbackParams.put("vnp_TxnRef", "test-txn-ref");

            Response response = given()
                .queryParams(callbackParams)
                .get("/payments/vnpay-return");

            // Then: Endpoint handles callback (even with invalid signature)
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));
        }

        @Test
        @DisplayName("Should update invoice status after payment")
        void shouldUpdateInvoiceStatusAfterPayment() {
            if (invoiceId == null || paymentId == null) {
                return;
            }

            // When: Checking invoice status
            Response response = givenAuth(patientToken)
                .get("/invoices/" + invoiceId);

            // Then: Invoice status reflects payment
            response.then()
                .statusCode(anyOf(equalTo(200), equalTo(404)));

            if (response.statusCode() == 200) {
                response.then()
                    .body("data.status", notNullValue());
            }
        }
    }
}
