package com.hms.report_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import com.hms.report_service.dtos.AppointmentStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Feign client for appointment-service.
 * Only uses /appointments/stats for pre-aggregated data.
 */
@FeignClient(name = "appointment-service", configuration = FeignConfig.class)
public interface AppointmentClient {

    /**
     * Get pre-aggregated appointment statistics from appointment-service.
     */
    @GetMapping("/appointments/stats")
    ApiResponse<AppointmentStatsDTO> getAppointmentStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );
}
