package com.hms.patient_service.hooks;

import com.hms.common.clients.AccountClient;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.enums.RoleEnum;
import com.hms.common.test.TestDataFactory;
import com.hms.patient_service.constants.Gender;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.repositories.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PatientHook.
 * Tests lifecycle hooks for Patient entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-PAT-003/004: PatientHook Unit Tests")
class PatientHookTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private PatientHook patientHook;

    private PatientRequest testRequest;
    private Patient testEntity;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        // Setup circuit breaker mock - execute supplier directly (pass-through behavior)
        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
        lenient().when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        // Create PatientHook manually after CB mocks are configured
        patientHook = new PatientHook(patientRepository, accountClient, circuitBreakerFactory);

        context = new HashMap<>();

        testRequest = new PatientRequest();
        testRequest.setAccountId(TestDataFactory.uuid());
        testRequest.setFullName(TestDataFactory.fullName());
        testRequest.setEmail(TestDataFactory.uniqueEmail());
        testRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
        testRequest.setGender(Gender.MALE);
        testRequest.setPhoneNumber("0912345678");
        testRequest.setAddress("123 Main St");
        testRequest.setIdentificationNumber("079090001234");
        testRequest.setHealthInsuranceNumber("HS1234567890");

        testEntity = new Patient();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setAccountId(testRequest.getAccountId());
        testEntity.setFullName(testRequest.getFullName());
        testEntity.setEmail(testRequest.getEmail());
        testEntity.setDateOfBirth(testRequest.getDateOfBirth());
        testEntity.setGender(testRequest.getGender());
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("UC-PAT-003: Should pass validation when patient does not exist")
        void validateCreate_whenPatientDoesNotExist_shouldPass() {
            // Given
            given(patientRepository.exists(any(Specification.class))).willReturn(false);

            // When & Then
            assertThatCode(() -> patientHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(patientRepository).should().exists(any(Specification.class));
        }

        @Test
        @DisplayName("UC-PAT-003: Should throw exception when patient with same email exists")
        void validateCreate_whenEmailExists_shouldThrowException() {
            // Given
            given(patientRepository.exists(any(Specification.class))).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> patientHook.validateCreate(testRequest, context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Patient already exists");

            then(patientRepository).should().exists(any(Specification.class));
        }

        @Test
        @DisplayName("Should throw exception when patient with same identification number exists")
        void validateCreate_whenIdentificationNumberExists_shouldThrowException() {
            // Given
            given(patientRepository.exists(any(Specification.class))).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> patientHook.validateCreate(testRequest, context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Patient already exists");
        }

        @Test
        @DisplayName("Should throw exception when patient with same health insurance number exists")
        void validateCreate_whenHealthInsuranceNumberExists_shouldThrowException() {
            // Given
            given(patientRepository.exists(any(Specification.class))).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> patientHook.validateCreate(testRequest, context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Patient already exists");
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("UC-PAT-003: Should enrich entity with default data when fields are null")
        void enrichCreate_withNullFields_shouldEnrichWithDefaults() {
            // Given
            testEntity.setAccountId(null); // Don't trigger account fetch
            testEntity.setEmail(null);
            testEntity.setPhoneNumber(null);
            testEntity.setAddress(null);
            testEntity.setIdentificationNumber(null);
            testEntity.setHealthInsuranceNumber(null);
            testEntity.setRelativeFullName(null);
            testEntity.setRelativePhoneNumber(null);

            // When
            patientHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getEmail()).isEqualTo("N/A");
            assertThat(testEntity.getPhoneNumber()).isEqualTo("N/A");
            assertThat(testEntity.getAddress()).isEqualTo("Việt Nam");
            assertThat(testEntity.getIdentificationNumber()).isEqualTo("N/A");
            assertThat(testEntity.getHealthInsuranceNumber()).isEqualTo("N/A");
            assertThat(testEntity.getRelativeFullName()).isEqualTo("N/A");
            assertThat(testEntity.getRelativePhoneNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should enrich entity with default data when fields are empty strings")
        void enrichCreate_withEmptyFields_shouldEnrichWithDefaults() {
            // Given
            testEntity.setAccountId(null); // Don't trigger account fetch
            testEntity.setEmail("");
            testEntity.setPhoneNumber("");
            testEntity.setAddress("");
            testEntity.setIdentificationNumber("");
            testEntity.setHealthInsuranceNumber("");

            // When
            patientHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getEmail()).isEqualTo("N/A");
            assertThat(testEntity.getPhoneNumber()).isEqualTo("N/A");
            assertThat(testEntity.getAddress()).isEqualTo("Việt Nam");
            assertThat(testEntity.getIdentificationNumber()).isEqualTo("N/A");
            assertThat(testEntity.getHealthInsuranceNumber()).isEqualTo("N/A");
        }

        @Test
        @DisplayName("Should preserve existing non-null field values")
        void enrichCreate_withExistingValues_shouldPreserveValues() {
            // Given
            String existingEmail = "patient@example.com";
            String existingPhone = "0912345678";
            testEntity.setEmail(existingEmail);
            testEntity.setPhoneNumber(existingPhone);

            // When
            patientHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getEmail()).isEqualTo(existingEmail);
            assertThat(testEntity.getPhoneNumber()).isEqualTo(existingPhone);
        }

        @Test
        @DisplayName("UC-PAT-003: Should auto-fill email from Account if accountId is provided and email is null")
        void enrichCreate_withAccountIdAndNoEmail_shouldFetchEmailFromAccount() {
            // Given
            String accountId = TestDataFactory.uuid();
            String accountEmail = "account@example.com";
            testEntity.setAccountId(accountId);
            testEntity.setEmail(null);

            AccountResponse accountResponse = new AccountResponse();
            accountResponse.setId(accountId);
            accountResponse.setEmail(accountEmail);
            accountResponse.setRole(RoleEnum.PATIENT);
            accountResponse.setEmailVerified(false);
            
            ApiResponse<AccountResponse> apiResponse = ApiResponse.ok(accountResponse);
            given(accountClient.findById(accountId)).willReturn(apiResponse);

            // When
            patientHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getEmail()).isEqualTo(accountEmail);
            then(accountClient).should().findById(accountId);
        }

        @Test
        @DisplayName("Should use N/A if account fetch fails")
        void enrichCreate_whenAccountFetchFails_shouldUseDefaultEmail() {
            // Given
            String accountId = TestDataFactory.uuid();
            testEntity.setAccountId(accountId);
            testEntity.setEmail(null);

            // When circuit breaker fallback is triggered (auth service down),
            // it throws RuntimeException as per the new implementation
            when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                    .thenAnswer(invocation -> {
                        Function<Throwable, ?> fallback = invocation.getArgument(1);
                        return fallback.apply(new RuntimeException("Connection refused"));
                    });

            // When & Then - Circuit breaker throws exception for service unavailability
            assertThatThrownBy(() -> patientHook.enrichCreate(testRequest, testEntity, context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Auth service unavailable");
        }

        @Test
        @DisplayName("Should not fetch account email if email is already set")
        void enrichCreate_withExistingEmail_shouldNotFetchFromAccount() {
            // Given
            String existingEmail = "existing@example.com";
            testEntity.setAccountId(TestDataFactory.uuid());
            testEntity.setEmail(existingEmail);

            // When
            patientHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getEmail()).isEqualTo(existingEmail);
            then(accountClient).should(never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Method: afterCreate()")
    class AfterCreateTests {

        @Test
        @DisplayName("UC-PAT-004: Should execute after create without errors")
        void afterCreate_shouldExecuteSuccessfully() {
            // Given
            PatientResponse response = new PatientResponse(
                    testEntity.getId(),
                    testEntity.getAccountId(),
                    testEntity.getFullName(),
                    testEntity.getEmail(),
                    testEntity.getDateOfBirth(),
                    testEntity.getGender(),
                    testEntity.getPhoneNumber(),
                    testEntity.getAddress(),
                    testEntity.getIdentificationNumber(),
                    testEntity.getHealthInsuranceNumber(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // When & Then
            assertThatCode(() -> patientHook.afterCreate(testEntity, response, context))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("Should pass validation when no duplicate patient exists")
        void validateUpdate_whenNoDuplicateExists_shouldPass() {
            // Given
            String patientId = testEntity.getId();
            Patient existingEntity = new Patient();
            existingEntity.setId(patientId);

            given(patientRepository.findAll(any(Specification.class))).willReturn(java.util.Collections.emptyList());

            // When & Then
            assertThatCode(() -> patientHook.validateUpdate(patientId, testRequest, existingEntity, context))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception when duplicate patient exists with different ID")
        void validateUpdate_whenDuplicateExistsWithDifferentId_shouldThrowException() {
            // Given
            String patientId = testEntity.getId();
            Patient existingEntity = new Patient();
            existingEntity.setId(patientId);

            Patient duplicatePatient = new Patient();
            duplicatePatient.setId("different-id");
            duplicatePatient.setEmail(testRequest.getEmail());

            given(patientRepository.findAll(any(Specification.class)))
                    .willReturn(java.util.Collections.singletonList(duplicatePatient));

            // When & Then
            assertThatThrownBy(() -> patientHook.validateUpdate(patientId, testRequest, existingEntity, context))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Patient already exists");
        }

        @Test
        @DisplayName("Should ignore email field in update request")
        void validateUpdate_shouldSetEmailToNull() {
            // Given
            String patientId = testEntity.getId();
            Patient existingEntity = new Patient();
            existingEntity.setId(patientId);
            testRequest.setEmail("newemail@example.com");

            given(patientRepository.findAll(any(Specification.class))).willReturn(java.util.Collections.emptyList());

            // When
            patientHook.validateUpdate(patientId, testRequest, existingEntity, context);

            // Then
            assertThat(testRequest.getEmail()).isNull(); // Email should be ignored in update
        }
    }

    @Nested
    @DisplayName("Method: enrichUpdate()")
    class EnrichUpdateTests {

        @Test
        @DisplayName("Should enrich entity with default data during update")
        void enrichUpdate_shouldEnrichWithDefaults() {
            // Given
            testEntity.setPhoneNumber(null);
            testEntity.setAddress(null);

            // When
            patientHook.enrichUpdate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPhoneNumber()).isEqualTo("N/A");
            assertThat(testEntity.getAddress()).isEqualTo("Việt Nam");
        }
    }
}
