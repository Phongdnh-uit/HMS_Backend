package com.hms.report_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import com.hms.report_service.dtos.InvoiceStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Feign client for billing-service.
 * Only uses /invoices/stats for pre-aggregated data (aggregation at source).
 */
@FeignClient(name = "billing-service", configuration = FeignConfig.class)
public interface BillingClient {

    /**
     * Get pre-aggregated invoice statistics from billing-service.
     */
    @GetMapping("/invoices/stats")
    ApiResponse<InvoiceStatsDTO> getInvoiceStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );
}
