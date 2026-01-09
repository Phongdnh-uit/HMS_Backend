package com.hms.patient_service.helpers;

import com.hms.common.test.TestDataFactory;
import com.hms.patient_service.constants.Gender;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.repositories.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for PatientHelper.
 * Tests utility methods for patient data enrichment and validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-PAT-005: PatientHelper Unit Tests")
class PatientHelperTest {

    @Mock
    private PatientRepository patientRepository;

    private Patient testPatient;
    private PatientRequest testRequest;

    @BeforeEach
    void setUp() {
        testPatient = new Patient();
        testPatient.setId(TestDataFactory.uuid());
        testPatient.setFullName(TestDataFactory.fullName());

        testRequest = new PatientRequest();
        testRequest.setFullName(TestDataFactory.fullName());
        testRequest.setEmail(TestDataFactory.uniqueEmail());
        testRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
        testRequest.setGender(Gender.MALE);
    }

    @Nested
    @DisplayName("Method: enrichDefaultData()")
    class EnrichDefaultDataTests {

        @Test
        @DisplayName("UC-PAT-005: Should set default email when null")
        void enrichDefaultData_withNullEmail_shouldSetDefault() {
            // Given
            testPatient.setEmail(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getEmail()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should set default email when empty")
        void enrichDefaultData_withEmptyEmail_shouldSetDefault() {
            // Given
            testPatient.setEmail("");

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getEmail()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should preserve existing email")
        void enrichDefaultData_withExistingEmail_shouldPreserve() {
            // Given
            String existingEmail = "patient@example.com";
            testPatient.setEmail(existingEmail);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getEmail()).isEqualTo(existingEmail);
        }

        @Test
        @DisplayName("Should set default phone number when null")
        void enrichDefaultData_withNullPhoneNumber_shouldSetDefault() {
            // Given
            testPatient.setPhoneNumber(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getPhoneNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should set default phone number when empty")
        void enrichDefaultData_withEmptyPhoneNumber_shouldSetDefault() {
            // Given
            testPatient.setPhoneNumber("");

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getPhoneNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should set default address when null")
        void enrichDefaultData_withNullAddress_shouldSetDefault() {
            // Given
            testPatient.setAddress(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getAddress()).isEqualTo("Việt Nam");
        }

        @Test
        @DisplayName("Should set default address when empty")
        void enrichDefaultData_withEmptyAddress_shouldSetDefault() {
            // Given
            testPatient.setAddress("");

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getAddress()).isEqualTo("Việt Nam");
        }

        @Test
        @DisplayName("Should set default identification number when null")
        void enrichDefaultData_withNullIdentificationNumber_shouldSetDefault() {
            // Given
            testPatient.setIdentificationNumber(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getIdentificationNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should set default health insurance number when null")
        void enrichDefaultData_withNullHealthInsuranceNumber_shouldSetDefault() {
            // Given
            testPatient.setHealthInsuranceNumber(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getHealthInsuranceNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should set default relative full name when null")
        void enrichDefaultData_withNullRelativeFullName_shouldSetDefault() {
            // Given
            testPatient.setRelativeFullName(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getRelativeFullName()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should set default relative phone number when null")
        void enrichDefaultData_withNullRelativePhoneNumber_shouldSetDefault() {
            // Given
            testPatient.setRelativePhoneNumber(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getRelativePhoneNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should enrich all null fields at once")
        void enrichDefaultData_withAllNullFields_shouldSetAllDefaults() {
            // Given
            testPatient.setEmail(null);
            testPatient.setPhoneNumber(null);
            testPatient.setAddress(null);
            testPatient.setIdentificationNumber(null);
            testPatient.setHealthInsuranceNumber(null);
            testPatient.setRelativeFullName(null);
            testPatient.setRelativePhoneNumber(null);

            // When
            Patient result = PatientHelper.enrichDefaultData(testPatient);

            // Then
            assertThat(result.getEmail()).isEqualTo("N/A");
            assertThat(result.getPhoneNumber()).isEqualTo("N/A");
            assertThat(result.getAddress()).isEqualTo("Việt Nam");
            assertThat(result.getIdentificationNumber()).isEqualTo("N/A");
            assertThat(result.getHealthInsuranceNumber()).isEqualTo("N/A");
            assertThat(result.getRelativeFullName()).isEqualTo("N/A");
            assertThat(result.getRelativePhoneNumber()).isEqualTo("N/A");
        }
    }

    @Nested
    @DisplayName("Method: isAccountExists() - Create")
    class IsAccountExistsCreateTests {

        @Test
        @DisplayName("UC-PAT-005: Should return true when patient with same email exists")
        void isAccountExists_whenEmailExists_shouldReturnTrue() {
            // Given
            testRequest.setEmail("existing@example.com");
            testRequest.setIdentificationNumber(null);
            testRequest.setHealthInsuranceNumber(null);

            given(patientRepository.exists(any(Specification.class))).willReturn(true);

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository);

            // Then
            assertThat(result).isTrue();
            then(patientRepository).should().exists(any(Specification.class));
        }

        @Test
        @DisplayName("Should return true when patient with same identification number exists")
        void isAccountExists_whenIdentificationNumberExists_shouldReturnTrue() {
            // Given
            testRequest.setEmail(null);
            testRequest.setIdentificationNumber("079090001234");
            testRequest.setHealthInsuranceNumber(null);

            given(patientRepository.exists(any(Specification.class))).willReturn(true);

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when patient with same health insurance number exists")
        void isAccountExists_whenHealthInsuranceNumberExists_shouldReturnTrue() {
            // Given
            testRequest.setEmail(null);
            testRequest.setIdentificationNumber(null);
            testRequest.setHealthInsuranceNumber("HS1234567890");

            given(patientRepository.exists(any(Specification.class))).willReturn(true);

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no patient exists")
        void isAccountExists_whenNoPatientExists_shouldReturnFalse() {
            // Given
            testRequest.setEmail("new@example.com");

            given(patientRepository.exists(any(Specification.class))).willReturn(false);

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when all identifying fields are null or empty")
        void isAccountExists_whenAllFieldsNullOrEmpty_shouldReturnFalse() {
            // Given
            testRequest.setEmail(null);
            testRequest.setIdentificationNumber(null);
            testRequest.setHealthInsuranceNumber(null);

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository);

            // Then
            assertThat(result).isFalse();
            then(patientRepository).should(never()).exists(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Method: isAccountExists() - Update")
    class IsAccountExistsUpdateTests {

        @Test
        @DisplayName("Should return false when duplicate is the same patient being updated")
        void isAccountExists_whenDuplicateIsSamePatient_shouldReturnFalse() {
            // Given
            String ownId = "patient-123";
            testRequest.setEmail("existing@example.com");

            Patient existingPatient = new Patient();
            existingPatient.setId(ownId);
            existingPatient.setEmail("existing@example.com");

            given(patientRepository.findAll(any(Specification.class)))
                    .willReturn(Collections.singletonList(existingPatient));

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository, ownId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when duplicate is a different patient")
        void isAccountExists_whenDuplicateIsDifferentPatient_shouldReturnTrue() {
            // Given
            String ownId = "patient-123";
            testRequest.setEmail("existing@example.com");

            Patient otherPatient = new Patient();
            otherPatient.setId("different-id");
            otherPatient.setEmail("existing@example.com");

            given(patientRepository.findAll(any(Specification.class)))
                    .willReturn(Collections.singletonList(otherPatient));

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository, ownId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no duplicate exists")
        void isAccountExists_whenNoDuplicateExists_shouldReturnFalse() {
            // Given
            String ownId = "patient-123";
            testRequest.setEmail("unique@example.com");

            given(patientRepository.findAll(any(Specification.class)))
                    .willReturn(Collections.emptyList());

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository, ownId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when all identifying fields are null")
        void isAccountExists_withUpdate_whenAllFieldsNull_shouldReturnFalse() {
            // Given
            String ownId = "patient-123";
            testRequest.setEmail(null);
            testRequest.setIdentificationNumber(null);
            testRequest.setHealthInsuranceNumber(null);

            // When
            boolean result = PatientHelper.isAccountExists(testRequest, patientRepository, ownId);

            // Then
            assertThat(result).isFalse();
            then(patientRepository).should(never()).findAll(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Specification Methods")
    class SpecificationTests {

        @Test
        @DisplayName("Should create email specification correctly")
        void emailLike_shouldCreateCorrectSpecification() {
            // Given
            String email = "test@example.com";

            // When
            Specification<Patient> spec = PatientHelper.emailLike(email);

            // Then
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create identification number specification correctly")
        void identificationNumberLike_shouldCreateCorrectSpecification() {
            // Given
            String identificationNumber = "079090001234";

            // When
            Specification<Patient> spec = PatientHelper.identificationNumberLike(identificationNumber);

            // Then
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Should create health insurance number specification correctly")
        void healthInsuranceNumberLike_shouldCreateCorrectSpecification() {
            // Given
            String healthInsuranceNumber = "HS1234567890";

            // When
            Specification<Patient> spec = PatientHelper.healthInsuranceNumberLike(healthInsuranceNumber);

            // Then
            assertThat(spec).isNotNull();
        }
    }
}
