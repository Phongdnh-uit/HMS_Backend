package com.hms.billing_service.mappers;

import com.hms.billing_service.dtos.PaymentResponse;
import com.hms.billing_service.entities.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "invoice.id", source = "invoice.id")
    @Mapping(target = "invoice.invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "invoice.totalAmount", source = "invoice.totalAmount")
    @Mapping(target = "invoice.status", source = "invoice.status")
    PaymentResponse entityToResponse(Payment entity);

    List<PaymentResponse> toResponseList(List<Payment> payments);
}
