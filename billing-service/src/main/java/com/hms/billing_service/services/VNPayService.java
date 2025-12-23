package com.hms.billing_service.services;

import com.hms.billing_service.config.VNPayConfig;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for VNPay payment gateway integration.
 * Handles URL generation, signature verification, and callback processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    private static final DateTimeFormatter VN_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    /**
     * Creates a VNPay payment URL for the given invoice.
     *
     * @param payment   Payment entity with invoice reference
     * @param clientIp  Client IP address
     * @param returnUrl Custom return URL (optional, uses default if null)
     * @param bankCode  Bank code for direct payment (optional)
     * @param language  Language code: vn or en
     * @return VNPay payment URL
     */
    public String createPaymentUrl(Payment payment, String clientIp, 
                                    String returnUrl, String bankCode, String language) {
        
        Invoice invoice = payment.getInvoice();
        
        // Amount in VND (VNPay requires amount * 100)
        long amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        
        // Create time
        String createDate = VN_DATE_FORMAT.format(Instant.now());
        String expireDate = VN_DATE_FORMAT.format(payment.getExpireAt());
        
        // Order info
        String orderInfo = payment.getOrderInfo() != null 
            ? payment.getOrderInfo() 
            : "Thanh toán Hóa đơn " + invoice.getInvoiceNumber();
        
        // Build parameters
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        params.put("vnp_TxnRef", payment.getTxnRef());
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", language != null ? language : "vn");
        params.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);
        
        if (bankCode != null && !bankCode.isEmpty()) {
            params.put("vnp_BankCode", bankCode);
        }
        
        // Build query string
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedValue = urlEncode(entry.getValue());
            query.append(entry.getKey()).append("=").append(encodedValue).append("&");
            hashData.append(entry.getKey()).append("=").append(encodedValue).append("&");
        }
        
        // Remove trailing &
        String queryString = query.substring(0, query.length() - 1);
        String hashDataString = hashData.substring(0, hashData.length() - 1);
        
        // Create signature
        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashDataString);
        
        return vnPayConfig.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Validates the signature from VNPay callback.
     *
     * @param params All callback parameters from VNPay
     * @return true if signature is valid
     */
    public boolean validateSignature(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            return false;
        }
        
        // Remove vnp_SecureHash and vnp_SecureHashType from params
        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");
        
        // Build hash data
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                hashData.append(entry.getKey()).append("=")
                       .append(urlEncode(entry.getValue())).append("&");
            }
        }
        
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1); // Remove trailing &
        }
        
        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        
        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }

    /**
     * Parses VNPay callback response code.
     *
     * @param responseCode VNPay response code
     * @return Human readable message
     */
    public String getResponseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ";
            case "09" -> "Giao dịch không thành công: Thẻ/Tài khoản chưa đăng ký Internet Banking";
            case "10" -> "Giao dịch không thành công: Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công: Đã hết thời gian chờ thanh toán";
            case "12" -> "Giao dịch không thành công: Thẻ/Tài khoản bị khóa";
            case "13" -> "Giao dịch không thành công: Nhập sai mật khẩu xác thực";
            case "24" -> "Giao dịch không thành công: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công: Tài khoản không đủ số dư";
            case "65" -> "Giao dịch không thành công: Tài khoản vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công: Nhập sai mật khẩu quá số lần quy định";
            case "99" -> "Lỗi không xác định";
            default -> "Lỗi không xác định: " + responseCode;
        };
    }

    /**
     * Checks if payment was successful based on response code.
     */
    public boolean isSuccessful(String responseCode) {
        return "00".equals(responseCode);
    }

    /**
     * Generates a unique transaction reference.
     */
    public String generateTxnRef() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * HMAC SHA512 signature.
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            log.error("Error creating HMAC SHA512", e);
            throw new RuntimeException("Error creating signature", e);
        }
    }

    /**
     * URL encode helper.
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }
}
