package com.hms.medical_exam_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for billing-service to generate invoices.
 * Used for auto-invoice generation after exam finalization or prescription dispense.
 */
@FeignClient(name = "billing-service", path = "/invoices")
public interface BillingClient {

    /**
     * Create or update an invoice for an appointment.
     * If invoice exists, it will be UPDATED with fresh items.
     * If not, a new invoice will be created.
     * 
     * @param request Contains appointmentId, examId and optional notes
     * @return Created or updated invoice response
     */
    @PostMapping("/upsert")
    ApiResponse<InvoiceResponse> upsertInvoice(@RequestBody InvoiceRequest request);

    // Request DTO matching billing-service InvoiceRequest
    record InvoiceRequest(
        String appointmentId,
        String examId,
        String notes
    ) {}

    // Simplified response - just need to know it was created
    record InvoiceResponse(
        String id,
        String invoiceNumber,
        String patientId,
        String patientName,
        String status
    ) {}
}
