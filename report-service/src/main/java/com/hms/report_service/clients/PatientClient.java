package com.hms.report_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import com.hms.report_service.dtos.PatientStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for patient-service.
 * Only uses /patients/stats for pre-aggregated data.
 */
@FeignClient(name = "patient-service", configuration = FeignConfig.class)
public interface PatientClient {

    /**
     * Get pre-aggregated patient statistics from patient-service.
     */
    @GetMapping("/patients/stats")
    ApiResponse<PatientStatsDTO> getPatientStats();
}
