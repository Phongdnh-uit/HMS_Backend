package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue report response DTO.
 * Contains aggregated financial data for a given period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse implements Serializable {
    
    private Period period;
    private BigDecimal totalRevenue;
    private BigDecimal paidRevenue;
    private BigDecimal unpaidRevenue;
    private InvoiceCount invoiceCount;
    private List<DepartmentRevenue> revenueByDepartment;
    private List<PaymentMethodRevenue> revenueByPaymentMethod;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period implements Serializable {
        private LocalDate startDate;
        private LocalDate endDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceCount implements Serializable {
        private int total;
        private int paid;
        private int unpaid;
        private int overdue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentRevenue implements Serializable {
        private String departmentId;
        private String departmentName;
        private BigDecimal revenue;
        private double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodRevenue implements Serializable {
        private String method;
        private BigDecimal amount;
        private double percentage;
    }
}
