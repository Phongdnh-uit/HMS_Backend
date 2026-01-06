package com.hms.report_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import com.hms.report_service.dtos.DiagnosisStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for medical-exam-service.
 * Uses /exams/stats for diagnosis statistics.
 */
@FeignClient(name = "medical-exam-service", configuration = FeignConfig.class)
public interface MedicalExamClient {

    /**
     * Get diagnosis statistics from medical-exam-service.
     */
    @GetMapping("/exams/stats")
    ApiResponse<DiagnosisStatsDTO> getDiagnosisStats();
}
