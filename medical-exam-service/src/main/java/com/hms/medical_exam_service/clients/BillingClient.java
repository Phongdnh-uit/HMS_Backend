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
     * Create an invoice for an appointment.
     * Billing-service will fetch exam/prescription details automatically.
     * 
     * @param request Contains appointmentId and optional notes
     * @return Created invoice response
     */
    @PostMapping
    ApiResponse<InvoiceResponse> createInvoice(@RequestBody InvoiceRequest request);

    // Request DTO matching billing-service InvoiceRequest
    record InvoiceRequest(
        String appointmentId,
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
