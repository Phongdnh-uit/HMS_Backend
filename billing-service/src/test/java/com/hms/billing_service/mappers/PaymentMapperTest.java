package com.hms.billing_service.mappers;

import com.hms.billing_service.dtos.PaymentResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.Payment;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PaymentMapper.
 * Tests MapStruct mapper methods for correct field mapping.
 * 
 * Note: MapStruct mappers are generated at compile time and need Spring context
 * for dependency injection when using componentModel = "spring".
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL"
})
@ActiveProfiles("test")
@DisplayName("UC-BILL-002: PaymentMapper Unit Tests")
class PaymentMapperTest {

    @Autowired
    private PaymentMapper mapper;

    private Payment testPayment;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        // Setup test invoice
        testInvoice = Invoice.builder()
                .id(TestDataFactory.uuid())
                .invoiceNumber("INV-20260107-0001")
                .totalAmount(new BigDecimal("500000"))
                .status(Invoice.InvoiceStatus.UNPAID)
                .build();

        // Setup test payment
        testPayment = Payment.builder()
                .id(TestDataFactory.uuid())
                .invoice(testInvoice)
                .txnRef("TXN-" + System.currentTimeMillis())
                .amount(new BigDecimal("500000"))
                .gateway(Payment.PaymentGateway.VNPAY)
                .status(Payment.PaymentStatus.PENDING)
                .orderInfo("Payment for invoice " + testInvoice.getInvoiceNumber())
                .notes("Test payment")
                .expireAt(Instant.now().plusSeconds(900))
                .build();
        
        testPayment.setCreatedAt(Instant.now());
        testPayment.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-BILL-002: Should map payment entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testPayment.getId());
            assertThat(response.getTxnRef()).isEqualTo(testPayment.getTxnRef());
            assertThat(response.getAmount()).isEqualByComparingTo(testPayment.getAmount());
            assertThat(response.getGateway()).isEqualTo(testPayment.getGateway());
            assertThat(response.getStatus()).isEqualTo(testPayment.getStatus());
            assertThat(response.getOrderInfo()).isEqualTo(testPayment.getOrderInfo());
            assertThat(response.getNotes()).isEqualTo(testPayment.getNotes());
            assertThat(response.getExpireAt()).isEqualTo(testPayment.getExpireAt());
            assertThat(response.getCreatedAt()).isEqualTo(testPayment.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testPayment.getUpdatedAt());
        }

        @Test
        @DisplayName("UC-BILL-002: Should map nested invoice info correctly")
        void entityToResponse_shouldMapInvoiceInfo() {
            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getInvoice()).isNotNull();
            assertThat(response.getInvoice().getId()).isEqualTo(testInvoice.getId());
            assertThat(response.getInvoice().getInvoiceNumber()).isEqualTo(testInvoice.getInvoiceNumber());
            assertThat(response.getInvoice().getTotalAmount()).isEqualByComparingTo(testInvoice.getTotalAmount());
            // Invoice status in response is a String (from enum name)
            assertThat(response.getInvoice().getStatus()).isEqualTo(testInvoice.getStatus().name());
        }

        @Test
        @DisplayName("UC-BILL-002: Should map VNPay specific fields")
        void entityToResponse_shouldMapVNPayFields() {
            // Given
            testPayment.setVnpTransactionNo("VNP-12345678");
            testPayment.setVnpBankCode("NCB");
            testPayment.setVnpCardType("ATM");
            testPayment.setVnpResponseCode("00");

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response.getVnpTransactionNo()).isEqualTo("VNP-12345678");
            assertThat(response.getVnpBankCode()).isEqualTo("NCB");
            assertThat(response.getVnpCardType()).isEqualTo("ATM");
            assertThat(response.getVnpResponseCode()).isEqualTo("00");
        }

        @Test
        @DisplayName("UC-BILL-002: Should map payment with SUCCESS status")
        void entityToResponse_withSuccessStatus_shouldMap() {
            // Given
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            testPayment.setPaymentDate(Instant.now());
            testPayment.setVnpTransactionNo("VNP-SUCCESS-123");

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
            assertThat(response.getPaymentDate()).isEqualTo(testPayment.getPaymentDate());
            assertThat(response.getVnpTransactionNo()).isEqualTo("VNP-SUCCESS-123");
        }

        @Test
        @DisplayName("UC-BILL-002: Should map payment with FAILED status")
        void entityToResponse_withFailedStatus_shouldMap() {
            // Given
            testPayment.setStatus(Payment.PaymentStatus.FAILED);
            testPayment.setVnpResponseCode("24");

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
            assertThat(response.getVnpResponseCode()).isEqualTo("24");
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            PaymentResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("UC-BILL-002: Should handle different payment gateways")
        void entityToResponse_withCashGateway_shouldMap() {
            // Given
            testPayment.setGateway(Payment.PaymentGateway.CASH);
            testPayment.setStatus(Payment.PaymentStatus.COMPLETED);

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response.getGateway()).isEqualTo(Payment.PaymentGateway.CASH);
            assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should handle payment with null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMap() {
            // Given
            testPayment.setNotes(null);
            testPayment.setPaymentDate(null);
            testPayment.setVnpTransactionNo(null);
            testPayment.setVnpBankCode(null);

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testPayment.getId());
            assertThat(response.getNotes()).isNull();
            assertThat(response.getPaymentDate()).isNull();
            assertThat(response.getVnpTransactionNo()).isNull();
            assertThat(response.getVnpBankCode()).isNull();
        }
    }

    @Nested
    @DisplayName("List Mapping")
    class ListMappingTests {

        @Test
        @DisplayName("UC-BILL-002: Should map list of payments to responses")
        void toResponseList_shouldMapAllPayments() {
            // Given
            Payment secondPayment = Payment.builder()
                    .id(TestDataFactory.uuid())
                    .invoice(testInvoice)
                    .txnRef("TXN-" + System.currentTimeMillis())
                    .amount(new BigDecimal("250000"))
                    .gateway(Payment.PaymentGateway.CASH)
                    .status(Payment.PaymentStatus.COMPLETED)
                    .build();
            
            List<Payment> payments = List.of(testPayment, secondPayment);

            // When
            List<PaymentResponse> responses = mapper.toResponseList(payments);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getId()).isEqualTo(testPayment.getId());
            assertThat(responses.get(0).getGateway()).isEqualTo(Payment.PaymentGateway.VNPAY);
            assertThat(responses.get(1).getId()).isEqualTo(secondPayment.getId());
            assertThat(responses.get(1).getGateway()).isEqualTo(Payment.PaymentGateway.CASH);
        }

        @Test
        @DisplayName("Should handle empty list")
        void toResponseList_withEmptyList_shouldReturnEmptyList() {
            // Given
            List<Payment> payments = List.of();

            // When
            List<PaymentResponse> responses = mapper.toResponseList(payments);

            // Then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("Should handle null list")
        void toResponseList_withNullList_shouldReturnNull() {
            // When
            List<PaymentResponse> responses = mapper.toResponseList(null);

            // Then
            assertThat(responses).isNull();
        }
    }

    @Nested
    @DisplayName("BigDecimal Precision Tests")
    class BigDecimalPrecisionTests {

        @Test
        @DisplayName("UC-BILL-002: Should preserve BigDecimal precision for amount")
        void entityToResponse_shouldPreserveBigDecimalPrecision() {
            // Given
            testPayment.setAmount(new BigDecimal("123456.78"));

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("123456.78"));
        }

        @Test
        @DisplayName("Should handle zero amount correctly")
        void entityToResponse_withZeroAmount_shouldMap() {
            // Given
            testPayment.setAmount(BigDecimal.ZERO);

            // When
            PaymentResponse response = mapper.entityToResponse(testPayment);

            // Then
            assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
