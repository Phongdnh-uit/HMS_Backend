package com.hms.billing_service.mappers;

import com.hms.billing_service.dtos.InvoiceItemResponse;
import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.dtos.InvoiceResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.InvoiceItem;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InvoiceMapper.
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
@DisplayName("UC-BILL-001: InvoiceMapper Unit Tests")
class InvoiceMapperTest {

    @Autowired
    private InvoiceMapper mapper;

    private Invoice testEntity;
    private InvoiceRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity with invoice items
        testEntity = Invoice.builder()
                .id(TestDataFactory.uuid())
                .invoiceNumber("INV-20260107-0001")
                .medicalExamId(TestDataFactory.uuid())
                .appointmentId(TestDataFactory.uuid())
                .patientId(TestDataFactory.uuid())
                .patientName(TestDataFactory.fullName())
                .invoiceDate(Instant.now())
                .dueDate(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .subtotal(new BigDecimal("500000"))
                .discount(new BigDecimal("50000"))
                .tax(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("500000"))
                .paidAmount(new BigDecimal("0"))
                .status(Invoice.InvoiceStatus.UNPAID)
                .items(new ArrayList<>())
                .build();
        
        testEntity.setCreatedAt(Instant.now());
        testEntity.setUpdatedAt(Instant.now());

        // Add invoice items
        InvoiceItem consultationItem = InvoiceItem.builder()
                .id(TestDataFactory.uuid())
                .invoice(testEntity)
                .type(InvoiceItem.ItemType.CONSULTATION)
                .description("Consultation Fee")
                .quantity(1)
                .unitPrice(new BigDecimal("200000"))
                .amount(new BigDecimal("200000"))
                .build();
        
        InvoiceItem medicineItem = InvoiceItem.builder()
                .id(TestDataFactory.uuid())
                .invoice(testEntity)
                .type(InvoiceItem.ItemType.MEDICINE)
                .description("Paracetamol 500mg")
                .referenceId(TestDataFactory.uuid())
                .quantity(10)
                .unitPrice(new BigDecimal("5000"))
                .amount(new BigDecimal("50000"))
                .build();
        
        testEntity.getItems().add(consultationItem);
        testEntity.getItems().add(medicineItem);

        // Setup test request
        testRequest = new InvoiceRequest();
        testRequest.setAppointmentId(TestDataFactory.uuid());
        testRequest.setExamId(TestDataFactory.uuid());
        testRequest.setNotes("Test invoice notes");
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-BILL-001: Should map invoice entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(testEntity.getId());
            assertThat(response.invoiceNumber()).isEqualTo(testEntity.getInvoiceNumber());
            assertThat(response.invoiceDate()).isEqualTo(testEntity.getInvoiceDate());
            assertThat(response.dueDate()).isEqualTo(testEntity.getDueDate());
            assertThat(response.subtotal()).isEqualByComparingTo(testEntity.getSubtotal());
            assertThat(response.discount()).isEqualByComparingTo(testEntity.getDiscount());
            assertThat(response.tax()).isEqualByComparingTo(testEntity.getTax());
            assertThat(response.totalAmount()).isEqualByComparingTo(testEntity.getTotalAmount());
            assertThat(response.paidAmount()).isEqualByComparingTo(testEntity.getPaidAmount());
            assertThat(response.status()).isEqualTo(Invoice.InvoiceStatus.UNPAID.name());
            assertThat(response.createdAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(response.updatedAt()).isEqualTo(testEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("UC-BILL-001: Should map nested patient info correctly")
        void entityToResponse_shouldMapPatientInfo() {
            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.patient()).isNotNull();
            assertThat(response.patient().id()).isEqualTo(testEntity.getPatientId());
            assertThat(response.patient().fullName()).isEqualTo(testEntity.getPatientName());
        }

        @Test
        @DisplayName("UC-BILL-001: Should map nested appointment info correctly")
        void entityToResponse_shouldMapAppointmentInfo() {
            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.appointment()).isNotNull();
            assertThat(response.appointment().id()).isEqualTo(testEntity.getAppointmentId());
            // Note: appointmentTime is null because Invoice doesn't store it
            assertThat(response.appointment().appointmentTime()).isNull();
        }

        @Test
        @DisplayName("UC-BILL-001: Should map nested medical exam info correctly")
        void entityToResponse_shouldMapMedicalExamInfo() {
            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.medicalExam()).isNotNull();
            assertThat(response.medicalExam().id()).isEqualTo(testEntity.getMedicalExamId());
        }

        @Test
        @DisplayName("UC-BILL-001: Should calculate balance due correctly")
        void entityToResponse_shouldCalculateBalanceDue() {
            // Given
            testEntity.setPaidAmount(new BigDecimal("200000"));

            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            BigDecimal expectedBalance = testEntity.getTotalAmount().subtract(testEntity.getPaidAmount());
            assertThat(response.balanceDue()).isEqualByComparingTo(expectedBalance);
            assertThat(response.balanceDue()).isEqualByComparingTo(new BigDecimal("300000"));
        }

        @Test
        @DisplayName("UC-BILL-001: Should map invoice items correctly")
        void entityToResponse_shouldMapInvoiceItems() {
            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.items()).isNotNull();
            assertThat(response.items()).hasSize(2);
            
            InvoiceItemResponse consultationItem = response.items().get(0);
            assertThat(consultationItem.id()).isEqualTo(testEntity.getItems().get(0).getId());
            assertThat(consultationItem.type()).isEqualTo(InvoiceItem.ItemType.CONSULTATION.name());
            assertThat(consultationItem.description()).isEqualTo("Consultation Fee");
            assertThat(consultationItem.quantity()).isEqualTo(1);
            assertThat(consultationItem.unitPrice()).isEqualByComparingTo(new BigDecimal("200000"));
            assertThat(consultationItem.amount()).isEqualByComparingTo(new BigDecimal("200000"));

            InvoiceItemResponse medicineItem = response.items().get(1);
            assertThat(medicineItem.type()).isEqualTo(InvoiceItem.ItemType.MEDICINE.name());
            assertThat(medicineItem.description()).isEqualTo("Paracetamol 500mg");
            assertThat(medicineItem.quantity()).isEqualTo(10);
            assertThat(medicineItem.referenceId()).isEqualTo(testEntity.getItems().get(1).getReferenceId());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            InvoiceResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should handle invoice without cancellation")
        void entityToResponse_withoutCancellation_shouldHaveNullCancellationInfo() {
            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.cancellation()).isNull();
        }

        @Test
        @DisplayName("UC-BILL-001: Should map cancellation info when present")
        void entityToResponse_withCancellation_shouldMapCancellationInfo() {
            // Given
            testEntity.setStatus(Invoice.InvoiceStatus.CANCELLED);
            testEntity.setCancelledAt(Instant.now());
            testEntity.setCancelledBy("admin");
            testEntity.setCancelReason("Patient requested cancellation");

            // When
            InvoiceResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.cancellation()).isNotNull();
            assertThat(response.cancellation().cancelledAt()).isEqualTo(testEntity.getCancelledAt());
            assertThat(response.cancellation().cancelledBy()).isEqualTo(testEntity.getCancelledBy());
            assertThat(response.cancellation().reason()).isEqualTo(testEntity.getCancelReason());
        }

        @Test
        @DisplayName("Should map list of entities to responses")
        void toResponseList_shouldMapAllEntities() {
            // Given
            Invoice secondInvoice = Invoice.builder()
                    .id(TestDataFactory.uuid())
                    .invoiceNumber("INV-20260107-0002")
                    .medicalExamId(TestDataFactory.uuid())
                    .appointmentId(TestDataFactory.uuid())
                    .patientId(TestDataFactory.uuid())
                    .patientName(TestDataFactory.fullName())
                    .invoiceDate(Instant.now())
                    .totalAmount(new BigDecimal("300000"))
                    .paidAmount(BigDecimal.ZERO)
                    .status(Invoice.InvoiceStatus.UNPAID)
                    .items(new ArrayList<>())
                    .build();
            
            List<Invoice> entities = List.of(testEntity, secondInvoice);

            // When
            List<InvoiceResponse> responses = mapper.toResponseList(entities);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(testEntity.getId());
            assertThat(responses.get(1).id()).isEqualTo(secondInvoice.getId());
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-BILL-001: Should map request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            Invoice entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getAppointmentId()).isEqualTo(testRequest.getAppointmentId());
            assertThat(entity.getNotes()).isEqualTo(testRequest.getNotes());
        }

        @Test
        @DisplayName("Should handle null request")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            Invoice entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should map request with minimal fields")
        void requestToEntity_withMinimalFields_shouldMap() {
            // Given
            InvoiceRequest minimalRequest = new InvoiceRequest();
            minimalRequest.setAppointmentId(TestDataFactory.uuid());

            // When
            Invoice entity = mapper.requestToEntity(minimalRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getAppointmentId()).isEqualTo(minimalRequest.getAppointmentId());
            assertThat(entity.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("Partial Update Mapping")
    class PartialUpdateTests {

        @Test
        @DisplayName("UC-BILL-001: Should update entity with request data")
        void partialUpdate_shouldUpdateFields() {
            // Given
            Invoice existingEntity = Invoice.builder()
                    .id(TestDataFactory.uuid())
                    .invoiceNumber("INV-OLD")
                    .appointmentId("old-appointment")
                    .notes("Old notes")
                    .build();

            // When
            mapper.partialUpdate(testRequest, existingEntity);

            // Then
            assertThat(existingEntity.getAppointmentId()).isEqualTo(testRequest.getAppointmentId());
            assertThat(existingEntity.getNotes()).isEqualTo(testRequest.getNotes());
            // ID should remain unchanged
            assertThat(existingEntity.getId()).isNotNull();
            assertThat(existingEntity.getInvoiceNumber()).isEqualTo("INV-OLD");
        }

        @Test
        @DisplayName("Should handle partial update with null values")
        void partialUpdate_withNullValues_shouldUpdate() {
            // Given
            Invoice existingEntity = Invoice.builder()
                    .id(TestDataFactory.uuid())
                    .appointmentId("old-appointment")
                    .notes("Old notes")
                    .build();
            
            InvoiceRequest updateRequest = new InvoiceRequest();
            updateRequest.setAppointmentId(TestDataFactory.uuid());
            updateRequest.setNotes(null);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getAppointmentId()).isEqualTo(updateRequest.getAppointmentId());
            // Note: MapStruct default behavior may overwrite with null
        }
    }

    @Nested
    @DisplayName("Invoice Item Mapping")
    class InvoiceItemMappingTests {

        @Test
        @DisplayName("UC-BILL-001: Should map invoice item to response")
        void toItemResponse_shouldMapCorrectly() {
            // Given
            InvoiceItem item = testEntity.getItems().get(0);

            // When
            InvoiceItemResponse response = mapper.toItemResponse(item);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(item.getId());
            assertThat(response.type()).isEqualTo(InvoiceItem.ItemType.CONSULTATION.name());
            assertThat(response.description()).isEqualTo(item.getDescription());
            assertThat(response.quantity()).isEqualTo(item.getQuantity());
            assertThat(response.unitPrice()).isEqualByComparingTo(item.getUnitPrice());
            assertThat(response.amount()).isEqualByComparingTo(item.getAmount());
        }

        @Test
        @DisplayName("Should map list of invoice items")
        void toItemResponseList_shouldMapAllItems() {
            // Given
            List<InvoiceItem> items = testEntity.getItems();

            // When
            List<InvoiceItemResponse> responses = mapper.toItemResponseList(items);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).type()).isEqualTo(InvoiceItem.ItemType.CONSULTATION.name());
            assertThat(responses.get(1).type()).isEqualTo(InvoiceItem.ItemType.MEDICINE.name());
        }
    }
}
