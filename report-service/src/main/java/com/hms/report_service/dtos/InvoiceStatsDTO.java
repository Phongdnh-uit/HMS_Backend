package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for receiving invoice stats from billing-service.
 * Matches the structure of InvoiceStatsResponse from billing-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatsDTO {
    
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
