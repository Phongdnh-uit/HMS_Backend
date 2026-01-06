package com.hms.billing_service.controllers;

import com.hms.billing_service.clients.PatientClient;
import com.hms.billing_service.dtos.CancelInvoiceRequest;
import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.dtos.InvoiceResponse;
import com.hms.billing_service.dtos.InvoiceStatsResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.Payment;
import com.hms.billing_service.hooks.InvoiceHook;
import com.hms.billing_service.mappers.InvoiceMapper;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.billing_service.repositories.PaymentRepository;
import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.helpers.FeignHelper;
import com.hms.common.securities.UserContext;
import com.hms.common.services.CrudService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/invoices")
public class InvoiceController extends GenericController<Invoice, String, InvoiceRequest, InvoiceResponse> {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceHook invoiceHook;
    private final InvoiceMapper invoiceMapper;
    private final PatientClient patientClient;

    public InvoiceController(
            CrudService<Invoice, String, InvoiceRequest, InvoiceResponse> service,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            InvoiceMapper invoiceMapper,
            InvoiceHook invoiceHook,
            PatientClient patientClient) {
        super(service);
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceMapper = invoiceMapper;
        this.invoiceHook = invoiceHook;
        this.patientClient = patientClient;
    }

    /**
     * Safely convert JPQL aggregate result to BigDecimal.
     * JPQL may return Long, Double, or BigDecimal depending on database and query.
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Legacy alias for create.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generate(@RequestBody InvoiceRequest request) {
        return create(request);
    }

    /**
     * Get invoices for the currently logged-in patient.
     * Patient self-service endpoint - uses accountId from JWT to lookup patientId.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices(
            @RequestParam(required = false) String status) {
        // Get current user from security context
        UserContext.User currentUser = UserContext.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED, "User not authenticated");
        }
        
        // Lookup patient by accountId
        String patientId;
        try {
            var patientResponse = FeignHelper.safeCall(
                () -> patientClient.getPatientByAccountId(currentUser.getId())
            );
            patientId = patientResponse.getData().id();
            log.info("Found patient {} for accountId {}", patientId, currentUser.getId());
        } catch (Exception e) {
            log.error("Failed to lookup patient for accountId {}: {}", currentUser.getId(), e.getMessage());
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, 
                "Patient profile not found. Please contact support.");
        }
        
        // Get invoices for this patient
        List<Invoice> invoices;
        if (status != null) {
            Invoice.InvoiceStatus invoiceStatus = Invoice.InvoiceStatus.valueOf(status);
            invoices = invoiceRepository.findByPatientIdAndStatus(patientId, invoiceStatus);
        } else {
            invoices = invoiceRepository.findByPatientId(patientId);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(invoiceMapper.toResponseList(invoices)));
    }
    
    /**
     * Create or update invoice with all items (Consultation + Medicine + Lab Tests).
     * If invoice exists, it will be UPDATED. If not, a new invoice will be created.
     * This is the preferred endpoint for auto-invoice generation from other services.
     */
    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<InvoiceResponse>> upsert(@RequestBody InvoiceRequest request) {
        Invoice invoice = invoiceHook.upsertInvoice(
            request.getAppointmentId(), 
            request.getExamId(), 
            request.getNotes()
        );
        InvoiceResponse response = invoiceMapper.entityToResponse(invoice);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get invoice by appointment ID.
     */
    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getByAppointment(
            @PathVariable String appointmentId) {
        Invoice invoice = invoiceRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND));
        
        return ResponseEntity.ok(ApiResponse.ok(invoiceMapper.entityToResponse(invoice)));
    }

    /**
     * Get invoice by medical exam ID.
     */
    @GetMapping("/by-exam/{examId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getByExam(@PathVariable String examId) {
        Invoice invoice = invoiceRepository.findByMedicalExamId(examId)
            .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND));
        
        return ResponseEntity.ok(ApiResponse.ok(invoiceMapper.entityToResponse(invoice)));
    }

    /**
     * List all invoices (paginated).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> listInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String patientId,
            Pageable pageable) {
        
        Page<Invoice> invoices = invoiceRepository.findAll(pageable);
        Page<InvoiceResponse> response = invoices.map(invoiceMapper::entityToResponse);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get aggregated invoice statistics for reporting.
     * Pre-aggregated at data source - efficient for reporting.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InvoiceStatsResponse>> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Convert LocalDate to Instant
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Get aggregated amounts
        Object[] amounts = invoiceRepository.sumAmountsInDateRange(startInstant, endInstant);
        // Safely convert aggregate results to BigDecimal (JPQL may return different number types)
        // Also handle case when no invoices exist (array may be null or have fewer elements)
        BigDecimal totalRevenue = (amounts != null && amounts.length > 0) ? toBigDecimal(amounts[0]) : BigDecimal.ZERO;
        BigDecimal paidRevenue = (amounts != null && amounts.length > 1) ? toBigDecimal(amounts[1]) : BigDecimal.ZERO;
        BigDecimal unpaidRevenue = totalRevenue.subtract(paidRevenue);
        int totalInvoices = (amounts != null && amounts.length > 2 && amounts[2] != null) ? ((Number) amounts[2]).intValue() : 0;
        
        // Get counts by status
        List<Object[]> statusCounts = invoiceRepository.countByStatusInDateRange(startInstant, endInstant);
        int paidCount = 0, unpaidCount = 0, overdueCount = 0, partiallyPaidCount = 0, cancelledCount = 0;
        for (Object[] row : statusCounts) {
            Invoice.InvoiceStatus status = (Invoice.InvoiceStatus) row[0];
            int count = ((Number) row[1]).intValue();
            switch (status) {
                case PAID -> paidCount = count;
                case UNPAID -> unpaidCount = count;
                case OVERDUE -> overdueCount = count;
                case PARTIALLY_PAID -> partiallyPaidCount = count;
                case CANCELLED -> cancelledCount = count;
            }
        }
        
        // Get payment method breakdown - this is the source of truth for payments received in period
        List<Object[]> paymentBreakdown = paymentRepository.sumByGatewayInDateRange(startInstant, endInstant);
        List<InvoiceStatsResponse.PaymentMethodStats> methodStats = new ArrayList<>();
        BigDecimal actualPaidRevenue = BigDecimal.ZERO; // Calculate from actual payments received
        
        for (Object[] row : paymentBreakdown) {
            Payment.PaymentGateway gateway = (Payment.PaymentGateway) row[0];
            BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            int count = ((Number) row[2]).intValue();
            actualPaidRevenue = actualPaidRevenue.add(amount);
            methodStats.add(InvoiceStatsResponse.PaymentMethodStats.builder()
                .method(gateway.name())
                .amount(amount)
                .count(count)
                .percentage(0.0) // Calculate percentage after we have total
                .build());
        }
        
        // Update percentages now that we have the total
        for (InvoiceStatsResponse.PaymentMethodStats stat : methodStats) {
            if (actualPaidRevenue.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = stat.getAmount().divide(actualPaidRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
                stat.setPercentage(percentage);
            }
        }
        
        // Use actualPaidRevenue (from payments) instead of invoice-based paidRevenue
        // totalRevenue = revenue from invoices created in period (for context)
        // paidRevenue = actual payments received in period (what matters for reporting)
        
        InvoiceStatsResponse stats = InvoiceStatsResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .totalRevenue(actualPaidRevenue) // Use actual payments as "revenue" for the period
            .paidRevenue(actualPaidRevenue)
            .unpaidRevenue(BigDecimal.ZERO) // All payments in this period are "paid" by definition
            .totalInvoices(totalInvoices)
            .paidInvoices(paidCount)
            .unpaidInvoices(unpaidCount)
            .overdueInvoices(overdueCount)
            .partiallyPaidInvoices(partiallyPaidCount)
            .cancelledInvoices(cancelledCount)
            .paymentMethodBreakdown(methodStats)
            .generatedAt(Instant.now())
            .build();
        
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    /**
     * Get invoices by patient ID.
     */
    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getByPatient(
            @PathVariable String patientId,
            @RequestParam(required = false) String status) {
        
        List<Invoice> invoices;
        if (status != null) {
            Invoice.InvoiceStatus invoiceStatus = Invoice.InvoiceStatus.valueOf(status);
            invoices = invoiceRepository.findByPatientIdAndStatus(patientId, invoiceStatus);
        } else {
            invoices = invoiceRepository.findByPatientId(patientId);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(invoiceMapper.toResponseList(invoices)));
    }

    /**
     * Cancel an invoice.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(
            @PathVariable String id,
            @Valid @RequestBody CancelInvoiceRequest request,
            @RequestHeader(value = "X-User-ID", required = false) String userId) {
        
        Invoice invoice = invoiceRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.INVOICE_NOT_FOUND, 
                "Invoice not found: " + id));
        
        // Validate cancellation
        invoiceHook.validateCancel(invoice);
        
        // Update invoice
        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        invoice.setCancelReason(request.getCancelReason());
        invoice.setCancelledAt(Instant.now());
        invoice.setCancelledBy(userId != null ? userId : "system");
        
        Invoice saved = invoiceRepository.save(invoice);
        
        return ResponseEntity.ok(ApiResponse.ok(invoiceMapper.entityToResponse(saved)));
    }
}