package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionResponse;
import com.hms.medical_exam_service.entities.Prescription;
import com.hms.medical_exam_service.entities.PrescriptionItem;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PrescriptionMapper.
 * Tests MapStruct mapper methods for correct field mapping.
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("UC-EXAM-003: PrescriptionMapper Unit Tests")
class PrescriptionMapperTest {

    @Autowired
    private PrescriptionMapper mapper;

    private Prescription testEntity;
    private PrescriptionRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new Prescription();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setMedicalExamId(TestDataFactory.uuid());
        testEntity.setStatus(Prescription.Status.ACTIVE);
        testEntity.setPatientId(TestDataFactory.uuid());
        testEntity.setPatientName("Jane Smith");
        testEntity.setDoctorId(TestDataFactory.uuid());
        testEntity.setDoctorName("Dr. Johnson");
        testEntity.setPrescribedAt(Instant.now());
        testEntity.setNotes("Take medications as directed");
        testEntity.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        testEntity.setCreatedBy("doctor123");

        // Add prescription items
        PrescriptionItem item1 = new PrescriptionItem();
        item1.setId(TestDataFactory.uuid());
        item1.setMedicineId("MED001");
        item1.setMedicineName("Paracetamol");
        item1.setQuantity(20);
        item1.setDosage("500mg");
        item1.setDurationDays(5);
        item1.setInstructions("Take twice daily with food");
        item1.setUnitPrice(new BigDecimal("2.50"));
        testEntity.addItem(item1);

        PrescriptionItem item2 = new PrescriptionItem();
        item2.setId(TestDataFactory.uuid());
        item2.setMedicineId("MED002");
        item2.setMedicineName("Amoxicillin");
        item2.setQuantity(15);
        item2.setDosage("250mg");
        item2.setDurationDays(7);
        item2.setInstructions("Take three times daily");
        item2.setUnitPrice(new BigDecimal("5.00"));
        testEntity.addItem(item2);

        // Setup test request
        testRequest = new PrescriptionRequest();
        testRequest.setNotes("Follow dosage instructions carefully");
        
        List<PrescriptionItemRequest> itemRequests = new ArrayList<>();
        PrescriptionItemRequest itemReq1 = new PrescriptionItemRequest();
        itemReq1.setMedicineId("MED003");
        itemReq1.setQuantity(10);
        itemReq1.setDosage("100mg");
        itemReq1.setDurationDays(10);
        itemReq1.setInstructions("Take once daily");
        itemRequests.add(itemReq1);
        
        testRequest.setItems(itemRequests);
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("Should map notes from PrescriptionRequest to Prescription entity")
        void requestToEntity_shouldMapNotes() {
            // When
            Prescription result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNotes()).isEqualTo(testRequest.getNotes());
        }

        @Test
        @DisplayName("Should ignore items in requestToEntity (handled by hook)")
        void requestToEntity_shouldIgnoreItems() {
            // When
            Prescription result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty(); // Items not mapped, handled by hook
        }

        @Test
        @DisplayName("Should handle null notes")
        void requestToEntity_withNullNotes_shouldMapSuccessfully() {
            // Given
            testRequest.setNotes(null);

            // When
            Prescription result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("Should handle empty notes")
        void requestToEntity_withEmptyNotes_shouldMapSuccessfully() {
            // Given
            testRequest.setNotes("");

            // When
            Prescription result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNotes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("Should map all fields from Prescription entity to PrescriptionResponse")
        void entityToResponse_withAllFields_shouldMapCorrectly() {
            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
            
            // Check medical exam info
            assertThat(result.getMedicalExam()).isNotNull();
            assertThat(result.getMedicalExam().getId()).isEqualTo(testEntity.getMedicalExamId());
            
            // Check patient info
            assertThat(result.getPatient()).isNotNull();
            assertThat(result.getPatient().getId()).isEqualTo(testEntity.getPatientId());
            assertThat(result.getPatient().getFullName()).isEqualTo(testEntity.getPatientName());
            
            // Check doctor info
            assertThat(result.getDoctor()).isNotNull();
            assertThat(result.getDoctor().getId()).isEqualTo(testEntity.getDoctorId());
            assertThat(result.getDoctor().getFullName()).isEqualTo(testEntity.getDoctorName());
            
            // Check status mapping (enum to string)
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            
            // Check other fields
            assertThat(result.getPrescribedAt()).isEqualTo(testEntity.getPrescribedAt());
            assertThat(result.getNotes()).isEqualTo(testEntity.getNotes());
            assertThat(result.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(result.getCreatedBy()).isEqualTo(testEntity.getCreatedBy());
            
            // Check item count expression
            assertThat(result.getItemCount()).isEqualTo(2);
            
            // Check items are mapped
            assertThat(result.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("Should map status as string correctly for all enum values")
        void entityToResponse_shouldMapStatusAsString() {
            // Test ACTIVE
            testEntity.setStatus(Prescription.Status.ACTIVE);
            PrescriptionResponse result = mapper.entityToResponse(testEntity);
            assertThat(result.getStatus()).isEqualTo("ACTIVE");

            // Test CANCELLED
            testEntity.setStatus(Prescription.Status.CANCELLED);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getStatus()).isEqualTo("CANCELLED");

            // Test DISPENSED
            testEntity.setStatus(Prescription.Status.DISPENSED);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getStatus()).isEqualTo("DISPENSED");
        }

        @Test
        @DisplayName("Should calculate itemCount correctly with empty items")
        void entityToResponse_withNoItems_shouldReturnZeroCount() {
            // Given
            testEntity.getItems().clear();

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getItemCount()).isZero();
        }

        @Test
        @DisplayName("Should handle null items list")
        void entityToResponse_withNullItems_shouldReturnZeroCount() {
            // Given
            Prescription entityWithNullItems = new Prescription();
            entityWithNullItems.setId(TestDataFactory.uuid());
            entityWithNullItems.setMedicalExamId(TestDataFactory.uuid());
            entityWithNullItems.setStatus(Prescription.Status.ACTIVE);
            entityWithNullItems.setPatientId(TestDataFactory.uuid());
            entityWithNullItems.setPatientName("Test Patient");
            entityWithNullItems.setDoctorId(TestDataFactory.uuid());
            entityWithNullItems.setDoctorName("Test Doctor");
            entityWithNullItems.setPrescribedAt(Instant.now());
            // items is null by default before initialization

            // When
            PrescriptionResponse result = mapper.entityToResponse(entityWithNullItems);

            // Then - Expression handles null safely
            assertThat(result.getItemCount()).isZero();
        }

        @Test
        @DisplayName("Should handle null entity")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            PrescriptionResponse result = mapper.entityToResponse(null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Cancellation Mapping")
    class CancellationMappingTests {

        @Test
        @DisplayName("Should map cancellation info when status is CANCELLED")
        void entityToResponse_withCancelledStatus_shouldMapCancellation() {
            // Given
            testEntity.setStatus(Prescription.Status.CANCELLED);
            testEntity.setCancelledAt(Instant.now());
            testEntity.setCancelledBy("admin123");
            testEntity.setCancelReason("Patient requested cancellation");

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getCancellation()).isNotNull();
            assertThat(result.getCancellation().getCancelledAt()).isEqualTo(testEntity.getCancelledAt());
            assertThat(result.getCancellation().getCancelledBy()).isEqualTo(testEntity.getCancelledBy());
            assertThat(result.getCancellation().getReason()).isEqualTo(testEntity.getCancelReason());
        }

        @Test
        @DisplayName("Should return null cancellation info when status is ACTIVE")
        void entityToResponse_withActiveStatus_shouldNotMapCancellation() {
            // Given
            testEntity.setStatus(Prescription.Status.ACTIVE);

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getCancellation()).isNull();
        }

        @Test
        @DisplayName("Should return null cancellation info when status is DISPENSED")
        void entityToResponse_withDispensedStatus_shouldNotMapCancellation() {
            // Given
            testEntity.setStatus(Prescription.Status.DISPENSED);

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getCancellation()).isNull();
        }
    }

    @Nested
    @DisplayName("Dispense Mapping")
    class DispenseMappingTests {

        @Test
        @DisplayName("Should map dispense info when status is DISPENSED")
        void entityToResponse_withDispensedStatus_shouldMapDispense() {
            // Given
            testEntity.setStatus(Prescription.Status.DISPENSED);
            testEntity.setDispensedAt(Instant.now());
            testEntity.setDispensedBy("pharmacist456");

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getDispense()).isNotNull();
            assertThat(result.getDispense().getDispensedAt()).isEqualTo(testEntity.getDispensedAt());
            assertThat(result.getDispense().getDispensedBy()).isEqualTo(testEntity.getDispensedBy());
        }

        @Test
        @DisplayName("Should return null dispense info when status is ACTIVE")
        void entityToResponse_withActiveStatus_shouldNotMapDispense() {
            // Given
            testEntity.setStatus(Prescription.Status.ACTIVE);

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getDispense()).isNull();
        }

        @Test
        @DisplayName("Should return null dispense info when status is CANCELLED")
        void entityToResponse_withCancelledStatus_shouldNotMapDispense() {
            // Given
            testEntity.setStatus(Prescription.Status.CANCELLED);

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getDispense()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long notes field")
        void mapping_withLongNotes_shouldMapCorrectly() {
            // Given
            String longNotes = "A".repeat(2000);
            testRequest.setNotes(longNotes);

            // When
            Prescription entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getNotes()).hasSize(2000);
        }

        @Test
        @DisplayName("Should map prescription with multiple items correctly")
        void entityToResponse_withMultipleItems_shouldMapAll() {
            // Given - testEntity already has 2 items from setUp

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getItemCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should preserve medical exam relationship")
        void mapping_shouldPreserveMedicalExamId() {
            // Given
            String examId = TestDataFactory.uuid();
            testEntity.setMedicalExamId(examId);

            // When
            PrescriptionResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getMedicalExam().getId()).isEqualTo(examId);
        }
    }
}
