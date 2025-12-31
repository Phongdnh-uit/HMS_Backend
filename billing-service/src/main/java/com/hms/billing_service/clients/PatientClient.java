package com.hms.billing_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client to fetch patient data from patient-service.
 * Used for patient self-service endpoints and fallback lookups.
 */
@FeignClient(name = "patient-service", path = "/patients")
public interface PatientClient {

    @GetMapping("/{id}")
    ApiResponse<PatientResponse> getPatientById(@PathVariable String id);

    /**
     * Find patient by their auth account ID.
     * Used for self-service endpoints (e.g., /invoices/my).
     */
    @GetMapping("/by-account")
    ApiResponse<PatientResponse> getPatientByAccountId(@RequestParam("accountId") String accountId);

    /**
     * Patient response DTO.
     */
    record PatientResponse(
        String id,
        String accountId,
        String fullName,
        String phoneNumber,
        String email
    ) {}
}
