package com.hms.medical_exam_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for patient-service.
 * Used for fetching patient info to enforce RBAC.
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
     * DTO for patient info from patient-service.
     */
    record PatientInfo(
            String id,
            String fullName,
            String phoneNumber,
            String accountId
    ) {}
}
