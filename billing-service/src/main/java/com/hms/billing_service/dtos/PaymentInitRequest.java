package com.hms.billing_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request to initialize a payment via VNPay gateway.
 */
@Getter
@Setter
public class PaymentInitRequest {

    @NotBlank(message = "Invoice ID is required")
    private String invoiceId;

    /**
     * Frontend URL to redirect after payment.
     * If not provided, will use default configured URL.
     */
    private String returnUrl;

    /**
     * Optional bank code for direct payment.
     * If empty, VNPay will show bank selection page.
     */
    private String bankCode;

    /**
     * Language: "vn" (Vietnamese) or "en" (English).
     * Default: vn
     */
    private String language = "vn";

    /**
     * Additional order information/notes.
     */
    private String orderInfo;

    /**
     * Optional payment amount for partial payment.
     * If not provided, defaults to remaining invoice balance.
     * Must be positive and not exceed remaining balance.
     */
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
}
