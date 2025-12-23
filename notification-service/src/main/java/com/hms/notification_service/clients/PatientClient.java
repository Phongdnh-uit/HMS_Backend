package com.hms.notification_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for patient-service.
 */
@FeignClient(name = "patient-service")
public interface PatientClient {

    /**
     * Get patient by ID to retrieve email for notification.
     */
    @GetMapping("/patients/{id}")
    ApiResponse<PatientInfo> getPatientById(@PathVariable("id") String id);

    /**
     * Patient record for notification purposes.
     */
    record PatientInfo(
            String id,
            String fullName,
            String email,
            String phoneNumber
    ) {}
}
