package com.hms.billing_service.dtos;

import com.hms.billing_service.entities.Payment;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment response DTO with full payment details.
 */
@Getter
@Setter
public class PaymentResponse {

    private String id;
    private InvoiceInfo invoice;
    private String txnRef;
    private BigDecimal amount;
    private Payment.PaymentGateway gateway;
    private Payment.PaymentStatus status;
    
    // VNPay details
    private String vnpTransactionNo;
    private String vnpBankCode;
    private String vnpCardType;
    private String vnpResponseCode;
    
    private String orderInfo;
    private String notes;
    private Instant paymentDate;
    private Instant expireAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Nested DTO for invoice info in payment response
     */
    @Getter
    @Setter
    public static class InvoiceInfo {
        private String id;
        private String invoiceNumber;
        private BigDecimal totalAmount;
        private String status;
    }
}
