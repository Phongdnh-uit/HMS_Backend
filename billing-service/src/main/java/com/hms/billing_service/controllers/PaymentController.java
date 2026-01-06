package com.hms.billing_service.controllers;

import com.hms.billing_service.config.VNPayConfig;
import com.hms.billing_service.dtos.PaymentInitRequest;
import com.hms.billing_service.dtos.PaymentInitResponse;
import com.hms.billing_service.dtos.PaymentResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.Payment;
import com.hms.billing_service.mappers.PaymentMapper;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.billing_service.repositories.PaymentRepository;
import com.hms.billing_service.services.VNPayService;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;
    private final VNPayService vnPayService;
    private final VNPayConfig vnPayConfig;

    /**
     * Initialize VNPay payment.
     * Returns VNPay redirect URL.
     */
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> initPayment(
            @Valid @RequestBody PaymentInitRequest request,
            HttpServletRequest httpRequest) {

        // Find invoice
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found: " + request.getInvoiceId()));

        // Validate invoice status
        validateInvoiceForPayment(invoice);

        // Calculate remaining balance
        BigDecimal totalPaid = paymentRepository.sumCompletedAmountByInvoiceId(invoice.getId());
        BigDecimal remainingBalance = invoice.getTotalAmount().subtract(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.INVOICE_ALREADY_PAID, "Invoice is already fully paid");
        }

        // Determine payment amount (use requested amount or default to remaining balance)
        BigDecimal paymentAmount = request.getAmount() != null ? request.getAmount() : remainingBalance;
        
        // Validate payment amount
        if (paymentAmount.compareTo(remainingBalance) > 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Payment amount (" + paymentAmount + ") exceeds remaining balance (" + remainingBalance + ")");
        }
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Payment amount must be positive");
        }

        // Create payment record
        Payment payment = Payment.builder()
                .invoice(invoice)
                .txnRef(vnPayService.generateTxnRef())
                .amount(paymentAmount)
                .gateway(Payment.PaymentGateway.VNPAY)
                .status(Payment.PaymentStatus.PENDING)
                .orderInfo(request.getOrderInfo() != null ? request.getOrderInfo() 
                        : "Thanh toán Hóa đơn " + invoice.getInvoiceNumber())
                .returnUrl(request.getReturnUrl())
                .expireAt(Instant.now().plusSeconds(vnPayConfig.getExpireMinutes() * 60L))
                .build();

        payment = paymentRepository.save(payment);

        // Get client IP
        String clientIp = getClientIp(httpRequest);

        // Generate VNPay URL
        String paymentUrl = vnPayService.createPaymentUrl(
                payment, clientIp, request.getReturnUrl(), 
                request.getBankCode(), request.getLanguage());

        // Update payment status to PROCESSING
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Build response
        PaymentInitResponse response = PaymentInitResponse.builder()
                .paymentId(payment.getId())
                .txnRef(payment.getTxnRef())
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(payment.getAmount())
                .paymentUrl(paymentUrl)
                .expireAt(payment.getExpireAt())
                .status(payment.getStatus().name())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * VNPay return URL handler.
     * Called when user is redirected back from VNPay.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<PaymentResponse>> vnpayReturn(
            @RequestParam Map<String, String> params) {

        log.info("VNPay return callback: {}", params);

        // Validate signature
        if (!vnPayService.validateSignature(params)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid VNPay signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found for txnRef: " + txnRef));

        // Update payment with VNPay response
        updatePaymentFromVNPayResponse(payment, params);

        return ResponseEntity.ok(ApiResponse.ok(paymentMapper.entityToResponse(payment)));
    }

    /**
     * VNPay IPN (Instant Payment Notification) handler.
     * Server-to-server callback from VNPay.
     */
    @PostMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params) {

        log.info("VNPay IPN callback: {}", params);

        Map<String, String> response = new HashMap<>();

        // Validate signature
        if (!vnPayService.validateSignature(params)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return ResponseEntity.ok(response);
        }

        String txnRef = params.get("vnp_TxnRef");
        String amount = params.get("vnp_Amount");
        String responseCode = params.get("vnp_ResponseCode");

        Payment payment = paymentRepository.findByTxnRef(txnRef).orElse(null);

        if (payment == null) {
            response.put("RspCode", "01");
            response.put("Message", "Order not found");
            return ResponseEntity.ok(response);
        }

        // Check amount
        long vnpAmount = Long.parseLong(amount) / 100;
        if (payment.getAmount().longValue() != vnpAmount) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid amount");
            return ResponseEntity.ok(response);
        }

        // Check if already processed
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return ResponseEntity.ok(response);
        }

        // Update payment
        updatePaymentFromVNPayResponse(payment, params);

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);
    }

    /**
     * Record a cash payment for an invoice.
     */
    @PostMapping("/{invoiceId}/cash")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordCashPayment(
            @PathVariable String invoiceId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String notes) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found: " + invoiceId));

        validateInvoiceForPayment(invoice);

        // Calculate remaining balance
        BigDecimal totalPaid = paymentRepository.sumCompletedAmountByInvoiceId(invoiceId);
        BigDecimal remainingBalance = invoice.getTotalAmount()
                .subtract(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.INVOICE_ALREADY_PAID, "Invoice is already fully paid");
        }

        // Default to remaining balance if amount not specified
        BigDecimal paymentAmount = amount != null ? amount : remainingBalance;

        if (paymentAmount.compareTo(remainingBalance) > 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Payment amount exceeds remaining balance: " + remainingBalance);
        }

        // Create cash payment
        Payment payment = Payment.builder()
                .invoice(invoice)
                .txnRef(vnPayService.generateTxnRef())
                .amount(paymentAmount)
                .gateway(Payment.PaymentGateway.CASH)
                .status(Payment.PaymentStatus.COMPLETED)
                .notes(notes)
                .paymentDate(Instant.now())
                .build();

        payment = paymentRepository.save(payment);

        // Update invoice status
        updateInvoiceStatus(invoice);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(paymentMapper.entityToResponse(payment)));
    }

    /**
     * Get payment by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found: " + id));

        return ResponseEntity.ok(ApiResponse.ok(paymentMapper.entityToResponse(payment)));
    }

    /**
     * Get all payments for an invoice.
     */
    @GetMapping("/by-invoice/{invoiceId}")
    public ResponseEntity<ApiResponse<PaymentsByInvoiceResponse>> getPaymentsByInvoice(
            @PathVariable String invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found: " + invoiceId));

        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        BigDecimal totalPaid = paymentRepository.sumCompletedAmountByInvoiceId(invoiceId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        BigDecimal remainingBalance = invoice.getTotalAmount().subtract(totalPaid);

        PaymentsByInvoiceResponse response = new PaymentsByInvoiceResponse();
        response.setPayments(paymentMapper.toResponseList(payments));
        response.setTotalPaid(totalPaid);
        response.setInvoiceTotal(invoice.getTotalAmount());
        response.setRemainingBalance(remainingBalance);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    /**
     * Get payment summary cards for dashboard.
     * Returns today's, this week's, cash and card payment statistics.
     */
    @GetMapping("/summary-cards")
    public ResponseEntity<ApiResponse<PaymentSummaryCardsResponse>> getPaymentSummaryCards() {
        // Calculate date ranges
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        
        Instant todayStart = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant weekStart = startOfWeek.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        
        // Get all completed payments
        List<Payment> allCompletedPayments = paymentRepository.findByStatus(Payment.PaymentStatus.COMPLETED);
        
        // Calculate today's payments
        BigDecimal todayAmount = BigDecimal.ZERO;
        long todayCount = 0;
        
        // Calculate this week's payments
        BigDecimal thisWeekAmount = BigDecimal.ZERO;
        long thisWeekCount = 0;
        
        // Calculate by gateway
        BigDecimal cashAmount = BigDecimal.ZERO;
        BigDecimal cardAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Payment payment : allCompletedPayments) {
            Instant paymentDate = payment.getPaymentDate();
            if (paymentDate == null) paymentDate = payment.getCreatedAt();
            
            totalAmount = totalAmount.add(payment.getAmount());
            
            // Check if today
            if (paymentDate != null && !paymentDate.isBefore(todayStart) && paymentDate.isBefore(todayEnd)) {
                todayAmount = todayAmount.add(payment.getAmount());
                todayCount++;
            }
            
            // Check if this week
            if (paymentDate != null && !paymentDate.isBefore(weekStart)) {
                thisWeekAmount = thisWeekAmount.add(payment.getAmount());
                thisWeekCount++;
            }
            
            // Sum by gateway
            if (payment.getGateway() == Payment.PaymentGateway.CASH) {
                cashAmount = cashAmount.add(payment.getAmount());
            } else {
                cardAmount = cardAmount.add(payment.getAmount());
            }
        }
        
        // Calculate percentages
        double cashPercentage = totalAmount.compareTo(BigDecimal.ZERO) > 0 
            ? cashAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 
            : 0;
        double cardPercentage = totalAmount.compareTo(BigDecimal.ZERO) > 0 
            ? cardAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 
            : 0;
        
        PaymentSummaryCardsResponse response = new PaymentSummaryCardsResponse();
        response.setTodayAmount(todayAmount);
        response.setTodayCount(todayCount);
        response.setThisWeekAmount(thisWeekAmount);
        response.setThisWeekCount(thisWeekCount);
        response.setCashAmount(cashAmount);
        response.setCashPercentage(cashPercentage);
        response.setCardAmount(cardAmount);
        response.setCardPercentage(cardPercentage);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ==================== Helper Methods ====================

    private void validateInvoiceForPayment(Invoice invoice) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new ApiException(ErrorCode.INVOICE_CANCELLED,
                    "Cannot pay a cancelled invoice");
        }
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new ApiException(ErrorCode.INVOICE_ALREADY_PAID,
                    "Invoice is already fully paid");
        }
    }

    private void updatePaymentFromVNPayResponse(Payment payment, Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        
        payment.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        payment.setVnpBankCode(params.get("vnp_BankCode"));
        payment.setVnpCardType(params.get("vnp_CardType"));
        payment.setVnpBankTranNo(params.get("vnp_BankTranNo"));
        payment.setVnpResponseCode(responseCode);
        payment.setVnpSecureHash(params.get("vnp_SecureHash"));

        if (vnPayService.isSuccessful(responseCode)) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentDate(Instant.now());
        } else if ("24".equals(responseCode)) {
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
        }

        // Save payment FIRST so that the sum query includes this payment
        paymentRepository.save(payment);
        
        // Then update invoice status with fresh totals
        if (vnPayService.isSuccessful(responseCode)) {
            updateInvoiceStatus(payment.getInvoice());
        }
    }

    private void updateInvoiceStatus(Invoice invoice) {
        BigDecimal totalPaid = paymentRepository.sumCompletedAmountByInvoiceId(invoice.getId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        
        invoice.setPaidAmount(totalPaid);

        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }

    @lombok.Getter
    @lombok.Setter
    public static class PaymentsByInvoiceResponse {
        private List<PaymentResponse> payments;
        private BigDecimal totalPaid;
        private BigDecimal invoiceTotal;
        private BigDecimal remainingBalance;
    }
    
    @lombok.Getter
    @lombok.Setter
    public static class PaymentSummaryCardsResponse {
        private BigDecimal todayAmount;
        private long todayCount;
        private BigDecimal thisWeekAmount;
        private long thisWeekCount;
        private BigDecimal cashAmount;
        private double cashPercentage;
        private BigDecimal cardAmount;
        private double cardPercentage;
    }
}
