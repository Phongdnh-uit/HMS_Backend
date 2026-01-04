package com.hms.medical_exam_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for patient-service.
 * Used for fetching patient info to enforce RBAC and self-service endpoints.
 */
@FeignClient(
        name = "patient-service",
        configuration = FeignConfig.class
)
public interface PatientClient {

    /**
     * Get current user's patient profile (for PATIENT role).
     * Requires X-User-ID header to be passed through via FeignConfig interceptor.
     */
    @GetMapping("/patients/me")
    ApiResponse<PatientInfo> getMyPatientProfile();

    /**
     * Find patient by their auth account ID.
     * Used for self-service endpoints (e.g., /exams/my, /lab-results/my).
     */
    @GetMapping("/patients/by-account")
    ApiResponse<PatientInfo> getPatientByAccountId(@RequestParam("accountId") String accountId);

    /**
     * DTO for patient info from patient-service.
     */
    record PatientInfo(
            String id,
            String fullName,
            String phoneNumber,
            String email,
            String accountId
    ) {}
}
