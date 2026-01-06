package com.hms.billing_service.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Invoice response following API contract with nested objects (Option B).
 */
public record InvoiceResponse(
    String id,
    String invoiceNumber,
    PatientInfo patient,
    AppointmentInfo appointment,
    MedicalExamInfo medicalExam,
    Instant invoiceDate,
    Instant dueDate,
    List<InvoiceItemResponse> items,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal tax,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal balanceDue,
    String status,
    String notes,
    CancellationInfo cancellation,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Nested DTO for patient info
     */
    public record PatientInfo(String id, String fullName) {}

    /**
     * Nested DTO for appointment info
     */
    public record AppointmentInfo(String id, Instant appointmentTime) {}

    /**
     * Nested DTO for medical exam info
     */
    public record MedicalExamInfo(String id) {}

    /**
     * Nested DTO for cancellation details
     */
    public record CancellationInfo(
        Instant cancelledAt,
        String cancelledBy,
        String reason
    ) {}
}
