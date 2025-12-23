package com.hms.billing_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VNPay configuration properties.
 * Configure via environment variables or application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VNPayConfig {

    /**
     * VNPay Merchant Code (TmnCode) from VNPay registration.
     */
    private String tmnCode;

    /**
     * VNPay Hash Secret Key for signature generation.
     */
    private String hashSecret;

    /**
     * VNPay Payment URL (sandbox or production).
     * Sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
     * Production: https://pay.vnpay.vn/vpcpay.html
     */
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /**
     * VNPay API URL for transaction queries.
     */
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    /**
     * Default return URL after payment.
     * This is your frontend URL that handles payment result.
     */
    private String returnUrl;

    /**
     * IPN URL for server-to-server notification.
     * VNPay will call this URL to confirm payment.
     */
    private String ipnUrl;

    /**
     * Payment link expiration time in minutes.
     * Default: 15 minutes.
     */
    private int expireMinutes = 15;

    /**
     * API version.
     */
    private String version = "2.1.0";

    /**
     * Command for payment.
     */
    private String command = "pay";

    /**
     * Currency code.
     */
    private String currCode = "VND";

    /**
     * Order type.
     * Default: other (250000 for healthcare)
     */
    private String orderType = "250000";
}
