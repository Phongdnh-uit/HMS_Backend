package com.hms.billing_service.mappers;

import com.hms.billing_service.dtos.InvoiceItemResponse;
import com.hms.billing_service.dtos.InvoiceResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.InvoiceItem;
import com.hms.common.mappers.GenericMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper extends GenericMapper<Invoice, com.hms.billing_service.dtos.InvoiceRequest, InvoiceResponse> {

    @Override
    @Mapping(target = "patient", expression = "java(toPatientInfo(entity))")
    @Mapping(target = "appointment", expression = "java(toAppointmentInfo(entity))")
    @Mapping(target = "medicalExam", expression = "java(toMedicalExamInfo(entity))")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "balanceDue", expression = "java(calculateBalanceDue(entity))")
    @Mapping(target = "cancellation", expression = "java(toCancellationInfo(entity))")
    @Mapping(target = "items", source = "items", qualifiedByName = "toItemResponseList")
    InvoiceResponse entityToResponse(Invoice entity);

    List<InvoiceResponse> toResponseList(List<Invoice> entities);

    @Override
    Invoice requestToEntity(com.hms.billing_service.dtos.InvoiceRequest request);

    @Override
    void partialUpdate(com.hms.billing_service.dtos.InvoiceRequest request, @MappingTarget Invoice entity);

    @Mapping(target = "type", expression = "java(item.getType().name())")
    InvoiceItemResponse toItemResponse(InvoiceItem item);

    @Named("toItemResponseList")
    List<InvoiceItemResponse> toItemResponseList(List<InvoiceItem> items);

    // Helper methods for nested objects
    default InvoiceResponse.PatientInfo toPatientInfo(Invoice entity) {
        return new InvoiceResponse.PatientInfo(entity.getPatientId(), entity.getPatientName());
    }

    default InvoiceResponse.AppointmentInfo toAppointmentInfo(Invoice entity) {
        // Note: appointmentTime is not stored in Invoice, would need to fetch
        // For now, return null for time - can be enhanced later if needed
        return new InvoiceResponse.AppointmentInfo(entity.getAppointmentId(), null);
    }

    default InvoiceResponse.MedicalExamInfo toMedicalExamInfo(Invoice entity) {
        return new InvoiceResponse.MedicalExamInfo(entity.getMedicalExamId());
    }

    default InvoiceResponse.CancellationInfo toCancellationInfo(Invoice entity) {
        if (entity.getCancelledAt() == null) {
            return null;
        }
        return new InvoiceResponse.CancellationInfo(
            entity.getCancelledAt(),
            entity.getCancelledBy(),
            entity.getCancelReason()
        );
    }

    // Helper to calculate balance due (total - paid)
    default java.math.BigDecimal calculateBalanceDue(Invoice entity) {
        java.math.BigDecimal total = entity.getTotalAmount() != null 
            ? entity.getTotalAmount() 
            : java.math.BigDecimal.ZERO;
        java.math.BigDecimal paid = entity.getPaidAmount() != null 
            ? entity.getPaidAmount() 
            : java.math.BigDecimal.ZERO;
        return total.subtract(paid);
    }
}
