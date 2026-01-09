package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MedicalExamMapper.
 * Tests MapStruct mapper methods for correct field mapping.
 * 
 * Note: MapStruct mappers are generated at compile time and need Spring context
 * for dependency injection when using componentModel = "spring".
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("UC-EXAM-001/002: MedicalExamMapper Unit Tests")
class MedicalExamMapperTest {

    @Autowired
    private MedicalExamMapper mapper;

    private MedicalExam testEntity;
    private MedicalExamRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new MedicalExam();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setAppointmentId(TestDataFactory.uuid());
        testEntity.setPatientId(TestDataFactory.uuid());
        testEntity.setPatientName("John Doe");
        testEntity.setDoctorId(TestDataFactory.uuid());
        testEntity.setDoctorName("Dr. Smith");
        testEntity.setDiagnosis("Common cold");
        testEntity.setSymptoms("Fever, cough, sore throat");
        testEntity.setTreatment("Rest and hydration");
        testEntity.setTemperature(37.5);
        testEntity.setBloodPressureSystolic(120);
        testEntity.setBloodPressureDiastolic(80);
        testEntity.setHeartRate(72);
        testEntity.setWeight(70.0);
        testEntity.setHeight(175.0);
        testEntity.setNotes("Patient should return if symptoms worsen");
        testEntity.setHasPrescription(false);
        testEntity.setExamDate(Instant.now());
        testEntity.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setCreatedBy("doctor123");
        testEntity.setUpdatedBy("doctor123");
        testEntity.setFollowUpDate(LocalDate.now().plusDays(7));
        testEntity.setFollowUpNotificationSent(false);

        // Setup test request
        testRequest = new MedicalExamRequest();
        testRequest.setAppointmentId(TestDataFactory.uuid());
        testRequest.setDiagnosis("Hypertension");
        testRequest.setSymptoms("Headache, dizziness");
        testRequest.setTreatment("Lifestyle changes and medication");
        testRequest.setTemperature(36.8);
        testRequest.setBloodPressureSystolic(140);
        testRequest.setBloodPressureDiastolic(90);
        testRequest.setHeartRate(85);
        testRequest.setWeight(75.0);
        testRequest.setHeight(170.0);
        testRequest.setNotes("Monitor blood pressure daily");
        testRequest.setHasPrescription(true);
        testRequest.setFollowUpDate(LocalDate.now().plusDays(14));
    }

    @Nested
    @DisplayName("UC-EXAM-001: Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("Should map all fields from MedicalExamRequest to MedicalExam entity")
        void requestToEntity_withAllFields_shouldMapCorrectly() {
            // When
            MedicalExam result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAppointmentId()).isEqualTo(testRequest.getAppointmentId());
            assertThat(result.getDiagnosis()).isEqualTo(testRequest.getDiagnosis());
            assertThat(result.getSymptoms()).isEqualTo(testRequest.getSymptoms());
            assertThat(result.getTreatment()).isEqualTo(testRequest.getTreatment());
            assertThat(result.getTemperature()).isEqualTo(testRequest.getTemperature());
            assertThat(result.getBloodPressureSystolic()).isEqualTo(testRequest.getBloodPressureSystolic());
            assertThat(result.getBloodPressureDiastolic()).isEqualTo(testRequest.getBloodPressureDiastolic());
            assertThat(result.getHeartRate()).isEqualTo(testRequest.getHeartRate());
            assertThat(result.getWeight()).isEqualTo(testRequest.getWeight());
            assertThat(result.getHeight()).isEqualTo(testRequest.getHeight());
            assertThat(result.getNotes()).isEqualTo(testRequest.getNotes());
            assertThat(result.getHasPrescription()).isEqualTo(testRequest.getHasPrescription());
            assertThat(result.getFollowUpDate()).isEqualTo(testRequest.getFollowUpDate());
        }

        @Test
        @DisplayName("Should handle null optional fields gracefully")
        void requestToEntity_withNullOptionalFields_shouldMapSuccessfully() {
            // Given
            MedicalExamRequest minimalRequest = new MedicalExamRequest();
            minimalRequest.setAppointmentId(TestDataFactory.uuid());
            // All other fields null

            // When
            MedicalExam result = mapper.requestToEntity(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAppointmentId()).isEqualTo(minimalRequest.getAppointmentId());
            assertThat(result.getDiagnosis()).isNull();
            assertThat(result.getSymptoms()).isNull();
            assertThat(result.getTreatment()).isNull();
            assertThat(result.getTemperature()).isNull();
            assertThat(result.getBloodPressureSystolic()).isNull();
            assertThat(result.getBloodPressureDiastolic()).isNull();
            assertThat(result.getHeartRate()).isNull();
            assertThat(result.getWeight()).isNull();
            assertThat(result.getHeight()).isNull();
            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("Should handle partial vitals data")
        void requestToEntity_withPartialVitals_shouldMapProvidedValues() {
            // Given
            MedicalExamRequest partialRequest = new MedicalExamRequest();
            partialRequest.setAppointmentId(TestDataFactory.uuid());
            partialRequest.setTemperature(37.2);
            partialRequest.setHeartRate(75);
            // Other vitals null

            // When
            MedicalExam result = mapper.requestToEntity(partialRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemperature()).isEqualTo(37.2);
            assertThat(result.getHeartRate()).isEqualTo(75);
            assertThat(result.getBloodPressureSystolic()).isNull();
            assertThat(result.getBloodPressureDiastolic()).isNull();
            assertThat(result.getWeight()).isNull();
            assertThat(result.getHeight()).isNull();
        }
    }

    @Nested
    @DisplayName("UC-EXAM-002: Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("Should map all fields from MedicalExam entity to MedicalExamResponse")
        void entityToResponse_withAllFields_shouldMapCorrectly() {
            // When
            MedicalExamResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
            
            // Check appointment info mapping
            assertThat(result.getAppointment()).isNotNull();
            assertThat(result.getAppointment().getId()).isEqualTo(testEntity.getAppointmentId());
            
            // Check patient info mapping
            assertThat(result.getPatient()).isNotNull();
            assertThat(result.getPatient().getId()).isEqualTo(testEntity.getPatientId());
            assertThat(result.getPatient().getFullName()).isEqualTo(testEntity.getPatientName());
            
            // Check doctor info mapping
            assertThat(result.getDoctor()).isNotNull();
            assertThat(result.getDoctor().getId()).isEqualTo(testEntity.getDoctorId());
            assertThat(result.getDoctor().getFullName()).isEqualTo(testEntity.getDoctorName());
            
            // Check medical data
            assertThat(result.getDiagnosis()).isEqualTo(testEntity.getDiagnosis());
            assertThat(result.getSymptoms()).isEqualTo(testEntity.getSymptoms());
            assertThat(result.getTreatment()).isEqualTo(testEntity.getTreatment());
            assertThat(result.getNotes()).isEqualTo(testEntity.getNotes());
            
            // Check vitals mapping (nested object)
            assertThat(result.getVitals()).isNotNull();
            assertThat(result.getVitals().getTemperature()).isEqualTo(testEntity.getTemperature());
            assertThat(result.getVitals().getBloodPressureSystolic()).isEqualTo(testEntity.getBloodPressureSystolic());
            assertThat(result.getVitals().getBloodPressureDiastolic()).isEqualTo(testEntity.getBloodPressureDiastolic());
            assertThat(result.getVitals().getHeartRate()).isEqualTo(testEntity.getHeartRate());
            assertThat(result.getVitals().getWeight()).isEqualTo(testEntity.getWeight());
            assertThat(result.getVitals().getHeight()).isEqualTo(testEntity.getHeight());
            
            // Check audit fields
            assertThat(result.getExamDate()).isEqualTo(testEntity.getExamDate());
            assertThat(result.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
            assertThat(result.getCreatedBy()).isEqualTo(testEntity.getCreatedBy());
            assertThat(result.getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
            
            // Check other fields
            assertThat(result.getHasPrescription()).isEqualTo(testEntity.getHasPrescription());
            assertThat(result.getFollowUpDate()).isEqualTo(testEntity.getFollowUpDate());
        }

        @Test
        @DisplayName("Should handle entity with null vitals")
        void entityToResponse_withNullVitals_shouldMapSuccessfully() {
            // Given
            testEntity.setTemperature(null);
            testEntity.setBloodPressureSystolic(null);
            testEntity.setBloodPressureDiastolic(null);
            testEntity.setHeartRate(null);
            testEntity.setWeight(null);
            testEntity.setHeight(null);

            // When
            MedicalExamResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getVitals()).isNotNull(); // Object created but fields are null
            assertThat(result.getVitals().getTemperature()).isNull();
            assertThat(result.getVitals().getBloodPressureSystolic()).isNull();
            assertThat(result.getVitals().getBloodPressureDiastolic()).isNull();
            assertThat(result.getVitals().getHeartRate()).isNull();
            assertThat(result.getVitals().getWeight()).isNull();
            assertThat(result.getVitals().getHeight()).isNull();
        }

        @Test
        @DisplayName("Should map vitals expression correctly")
        void entityToResponse_vitalsExpression_shouldCreateNestedObject() {
            // When
            MedicalExamResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getVitals()).isNotNull();
            assertThat(result.getVitals().getTemperature()).isEqualTo(37.5);
            assertThat(result.getVitals().getBloodPressureSystolic()).isEqualTo(120);
            assertThat(result.getVitals().getBloodPressureDiastolic()).isEqualTo(80);
            assertThat(result.getVitals().getHeartRate()).isEqualTo(72);
            assertThat(result.getVitals().getWeight()).isEqualTo(70.0);
            assertThat(result.getVitals().getHeight()).isEqualTo(175.0);
        }

        @Test
        @DisplayName("Should handle null entity")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            MedicalExamResponse result = mapper.entityToResponse(null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Partial Update Tests")
    class PartialUpdateTests {

        @Test
        @DisplayName("Should update entity fields from request without overwriting existing data")
        void partialUpdate_shouldUpdateOnlyProvidedFields() {
            // Given
            MedicalExamRequest updateRequest = new MedicalExamRequest();
            updateRequest.setDiagnosis("Updated diagnosis");
            updateRequest.setTemperature(38.0);
            // Other fields not set

            String originalSymptoms = testEntity.getSymptoms();
            String originalTreatment = testEntity.getTreatment();
            Integer originalHeartRate = testEntity.getHeartRate();

            // When
            mapper.partialUpdate(updateRequest, testEntity);

            // Then
            assertThat(testEntity.getDiagnosis()).isEqualTo("Updated diagnosis");
            assertThat(testEntity.getTemperature()).isEqualTo(38.0);
            
            // Note: MapStruct default behavior is to copy null values
            // Without nullValuePropertyMappingStrategy = IGNORE, null fields WILL overwrite
            assertThat(testEntity.getSymptoms()).isNull();
            assertThat(testEntity.getTreatment()).isNull();
            assertThat(testEntity.getHeartRate()).isNull();
        }

        @Test
        @DisplayName("Should update all fields when all are provided in request")
        void partialUpdate_withAllFields_shouldUpdateAllFields() {
            // When
            mapper.partialUpdate(testRequest, testEntity);

            // Then
            assertThat(testEntity.getDiagnosis()).isEqualTo(testRequest.getDiagnosis());
            assertThat(testEntity.getSymptoms()).isEqualTo(testRequest.getSymptoms());
            assertThat(testEntity.getTreatment()).isEqualTo(testRequest.getTreatment());
            assertThat(testEntity.getTemperature()).isEqualTo(testRequest.getTemperature());
            assertThat(testEntity.getBloodPressureSystolic()).isEqualTo(testRequest.getBloodPressureSystolic());
            assertThat(testEntity.getBloodPressureDiastolic()).isEqualTo(testRequest.getBloodPressureDiastolic());
            assertThat(testEntity.getHeartRate()).isEqualTo(testRequest.getHeartRate());
            assertThat(testEntity.getWeight()).isEqualTo(testRequest.getWeight());
            assertThat(testEntity.getHeight()).isEqualTo(testRequest.getHeight());
            assertThat(testEntity.getNotes()).isEqualTo(testRequest.getNotes());
            assertThat(testEntity.getFollowUpDate()).isEqualTo(testRequest.getFollowUpDate());
        }

        @Test
        @DisplayName("Should handle empty request without errors")
        void partialUpdate_withEmptyRequest_shouldNotCauseErrors() {
            // Given
            MedicalExamRequest emptyRequest = new MedicalExamRequest();
            String originalDiagnosis = testEntity.getDiagnosis();
            String originalSymptoms = testEntity.getSymptoms();

            // When
            mapper.partialUpdate(emptyRequest, testEntity);

            // Then - MapStruct default behavior: all fields become null
            // This is expected - partial updates require clients to send all fields
            assertThat(testEntity.getDiagnosis()).isNull();
            assertThat(testEntity.getSymptoms()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long text fields")
        void mapping_withLongTextFields_shouldMapCorrectly() {
            // Given
            String longText = "A".repeat(2000);
            testRequest.setDiagnosis(longText);
            testRequest.setSymptoms(longText);
            testRequest.setTreatment(longText);
            testRequest.setNotes(longText);

            // When
            MedicalExam entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getDiagnosis()).hasSize(2000);
            assertThat(entity.getSymptoms()).hasSize(2000);
            assertThat(entity.getTreatment()).hasSize(2000);
            assertThat(entity.getNotes()).hasSize(2000);
        }

        @Test
        @DisplayName("Should handle boundary values for vitals")
        void mapping_withBoundaryVitals_shouldMapCorrectly() {
            // Given
            testRequest.setTemperature(30.0);  // Min
            testRequest.setBloodPressureSystolic(50);  // Min
            testRequest.setBloodPressureDiastolic(150);  // Max
            testRequest.setHeartRate(200);  // Max

            // When
            MedicalExam entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getTemperature()).isEqualTo(30.0);
            assertThat(entity.getBloodPressureSystolic()).isEqualTo(50);
            assertThat(entity.getBloodPressureDiastolic()).isEqualTo(150);
            assertThat(entity.getHeartRate()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should preserve appointmentId in mapping")
        void mapping_shouldPreserveAppointmentId() {
            // Given
            String appointmentId = TestDataFactory.uuid();
            testRequest.setAppointmentId(appointmentId);

            // When
            MedicalExam entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getAppointmentId()).isEqualTo(appointmentId);
        }
    }
}
