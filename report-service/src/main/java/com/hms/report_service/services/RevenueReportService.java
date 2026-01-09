package com.hms.report_service.services;

import com.hms.report_service.clients.BillingClient;
import com.hms.report_service.dtos.InvoiceStatsDTO;
import com.hms.report_service.dtos.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
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
 * 
 * Caching strategy:
 * - Fresh data cached for 15 minutes (TTL)
 * - Degraded responses (dataStatus != null) are NOT cached
 * - When TTL expires + downstream unavailable -> returns UNAVAILABLE (honest failure)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueReportService {

    private final BillingClient billingClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /**
     * Generate revenue report for the specified period.
     * Calls billing-service /invoices/stats endpoint for pre-aggregated data.
     * Results are cached in Redis for 15 minutes.
     * Note: Degraded responses (dataStatus != null) are NOT cached.
     */
    @Cacheable(value = "revenue-reports", key = "#startDate + '-' + #endDate + '-' + #departmentId", unless = "#result.dataStatus != null")
    public RevenueReportResponse generateRevenueReport(LocalDate startDate, LocalDate endDate, String departmentId) {
        log.info("Generating revenue report from {} to {} (fetching from billing-service)", startDate, endDate);
        
        // Call billing-service stats endpoint - aggregation happens at data source
        var circuitBreaker = circuitBreakerFactory.create("reportBilling");
        var statsResponse = circuitBreaker.run(
            () -> billingClient.getInvoiceStats(startDate, endDate),
            throwable -> {
                log.warn("[CB-FALLBACK] Billing service unavailable: {}", throwable.getMessage());
                return null;
            }
        );
        
        if (statsResponse == null || statsResponse.getData() == null) {
            log.warn("No stats data returned from billing-service");
            return buildUnavailableReport(startDate, endDate, "Billing service unavailable");
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
    
    /**
     * Build report indicating data is unavailable (not misleading zeros).
     * This response is NOT cached (via unless condition).
     */
    private RevenueReportResponse buildUnavailableReport(LocalDate startDate, LocalDate endDate, String reason) {
        log.warn("Building unavailable revenue report: {}", reason);
        return RevenueReportResponse.builder()
            .period(RevenueReportResponse.Period.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .totalRevenue(null)  // null indicates unavailable, not zero
            .paidRevenue(null)
            .unpaidRevenue(null)
            .invoiceCount(null)  // null indicates unavailable
            .revenueByDepartment(new ArrayList<>())
            .revenueByPaymentMethod(new ArrayList<>())
            .generatedAt(Instant.now())
            .dataStatus("UNAVAILABLE: " + reason)
            .build();
    }
}

