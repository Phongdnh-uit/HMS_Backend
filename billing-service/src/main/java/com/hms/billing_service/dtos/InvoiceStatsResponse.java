package com.hms.billing_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for invoice statistics/revenue report.
 * Pre-aggregated at the data source for efficient reporting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatsResponse {
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    private BigDecimal totalRevenue;
    private BigDecimal paidRevenue;
    private BigDecimal unpaidRevenue;
    
    private int totalInvoices;
    private int paidInvoices;
    private int unpaidInvoices;
    private int overdueInvoices;
    private int partiallyPaidInvoices;
    private int cancelledInvoices;
    
    private List<PaymentMethodStats> paymentMethodBreakdown;
    
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodStats {
        private String method;
        private BigDecimal amount;
        private int count;
        private double percentage;
    }
}
