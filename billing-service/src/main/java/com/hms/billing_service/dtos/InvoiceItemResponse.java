package com.hms.billing_service.dtos;

import java.math.BigDecimal;

/**
 * Invoice item response DTO.
 */
public record InvoiceItemResponse(
    String id,
    String type,
    String description,
    String referenceId,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal amount
) {}
