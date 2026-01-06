package com.hms.appointment_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for patient-service.
 * Used for:
 * - Validating patient exists
 * - Fetching patient name for snapshot
 */
@FeignClient(
        name = "patient-service",
        configuration = FeignConfig.class
)
public interface PatientClient {

    /**
     * Get patient by ID.
     */
    @GetMapping("/patients/{id}")
    ApiResponse<PatientInfo> getPatientById(@PathVariable("id") String patientId);

    /**
     * Get current user's patient profile (for PATIENT role).
     * Requires X-User-ID header to be passed through.
     */
    @GetMapping("/patients/me")
    ApiResponse<PatientInfo> getMyPatientProfile();

    /**
     * DTO for patient info from patient-service.
     */
    record PatientInfo(
            String id,
            String fullName,
            String phoneNumber,
            String accountId
    ) {}
}
