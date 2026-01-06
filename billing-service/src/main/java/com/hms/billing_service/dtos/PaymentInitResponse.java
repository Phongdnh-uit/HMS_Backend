package com.hms.billing_service.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response after initializing a VNPay payment.
 * Contains the redirect URL to VNPay gateway.
 */
@Getter
@Setter
@Builder
public class PaymentInitResponse {

    private String paymentId;
    private String txnRef;
    private String invoiceId;
    private String invoiceNumber;
    private BigDecimal amount;
    
    /**
     * VNPay redirect URL - redirect user to this URL to complete payment.
     */
    private String paymentUrl;
    
    /**
     * When the payment link expires.
     */
    private Instant expireAt;
    
    private String status;
}
