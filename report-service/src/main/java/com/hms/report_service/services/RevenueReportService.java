package com.hms.report_service.services;

import com.hms.report_service.clients.BillingClient;
import com.hms.report_service.dtos.InvoiceStatsDTO;
import com.hms.report_service.dtos.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating revenue reports.
 * Uses Redis caching for performance optimization.
 * Aggregation is done at billing-service (data source) - this service just caches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueReportService {

    private final BillingClient billingClient;

    /**
     * Generate revenue report for the specified period.
     * Calls billing-service /invoices/stats endpoint for pre-aggregated data.
     * Results are cached in Redis for 15 minutes.
     */
    @Cacheable(value = "revenue-reports", key = "#startDate + '-' + #endDate + '-' + #departmentId")
    public RevenueReportResponse generateRevenueReport(LocalDate startDate, LocalDate endDate, String departmentId) {
        log.info("Generating revenue report from {} to {} (fetching from billing-service)", startDate, endDate);
        
        // Call billing-service stats endpoint - aggregation happens at data source
        var statsResponse = billingClient.getInvoiceStats(startDate, endDate);
        
        if (statsResponse == null || statsResponse.getData() == null) {
            log.warn("No stats data returned from billing-service");
            return buildEmptyReport(startDate, endDate);
        }
        
        InvoiceStatsDTO stats = statsResponse.getData();
        
        // Map payment method breakdown
        List<RevenueReportResponse.PaymentMethodRevenue> methodRevenues = new ArrayList<>();
        if (stats.getPaymentMethodBreakdown() != null) {
            methodRevenues = stats.getPaymentMethodBreakdown().stream()
                .map(m -> RevenueReportResponse.PaymentMethodRevenue.builder()
                    .method(m.getMethod())
                    .amount(m.getAmount())
                    .percentage(m.getPercentage())
                    .build())
                .collect(Collectors.toList());
        }

        return RevenueReportResponse.builder()
            .period(RevenueReportResponse.Period.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .totalRevenue(stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO)
            .paidRevenue(stats.getPaidRevenue() != null ? stats.getPaidRevenue() : BigDecimal.ZERO)
            .unpaidRevenue(stats.getUnpaidRevenue() != null ? stats.getUnpaidRevenue() : BigDecimal.ZERO)
            .invoiceCount(RevenueReportResponse.InvoiceCount.builder()
                .total(stats.getTotalInvoices())
                .paid(stats.getPaidInvoices())
                .unpaid(stats.getUnpaidInvoices())
                .overdue(stats.getOverdueInvoices())
                .build())
            .revenueByDepartment(new ArrayList<>()) // TODO: Add when department data is available
            .revenueByPaymentMethod(methodRevenues)
            .generatedAt(Instant.now())
            .build();
    }

    /**
     * Clear the revenue report cache.
     */
    @CacheEvict(value = "revenue-reports", allEntries = true)
    public void clearCache() {
        log.info("Revenue report cache cleared");
    }
    
    private RevenueReportResponse buildEmptyReport(LocalDate startDate, LocalDate endDate) {
        return RevenueReportResponse.builder()
            .period(RevenueReportResponse.Period.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .totalRevenue(BigDecimal.ZERO)
            .paidRevenue(BigDecimal.ZERO)
            .unpaidRevenue(BigDecimal.ZERO)
            .invoiceCount(RevenueReportResponse.InvoiceCount.builder().build())
            .revenueByDepartment(new ArrayList<>())
            .revenueByPaymentMethod(new ArrayList<>())
            .generatedAt(Instant.now())
            .build();
    }
}

