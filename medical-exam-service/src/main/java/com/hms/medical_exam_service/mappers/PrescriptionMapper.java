package com.hms.medical_exam_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionResponse;
import com.hms.medical_exam_service.entities.Prescription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {PrescriptionItemMapper.class})
public interface PrescriptionMapper extends GenericMapper<Prescription, PrescriptionRequest, PrescriptionResponse> {

    @Override
    @Mapping(target = "items", ignore = true)  // Items handled in hook (need medicine snapshots)
    Prescription requestToEntity(PrescriptionRequest request);

    @Override
    @Mapping(target = "medicalExam.id", source = "medicalExamId")
    @Mapping(target = "patient.id", source = "patientId")
    @Mapping(target = "patient.fullName", source = "patientName")
    @Mapping(target = "doctor.id", source = "doctorId")
    @Mapping(target = "doctor.fullName", source = "doctorName")
    @Mapping(target = "itemCount", expression = "java(entity.getItems() != null ? entity.getItems().size() : 0)")
    // Status as string for frontend flexibility
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    // Cancellation info - only mapped when status is CANCELLED
    @Mapping(target = "cancellation", expression = "java(mapCancellation(entity))")
    PrescriptionResponse entityToResponse(Prescription entity);

    // No partialUpdate - prescriptions are immutable
    
    /**
     * Maps cancellation info - returns null if prescription is not cancelled
     */
    default PrescriptionResponse.CancellationInfo mapCancellation(Prescription entity) {
        if (entity.getStatus() != Prescription.Status.CANCELLED) {
            return null;
        }
        PrescriptionResponse.CancellationInfo info = new PrescriptionResponse.CancellationInfo();
        info.setCancelledAt(entity.getCancelledAt());
        info.setCancelledBy(entity.getCancelledBy());
        info.setReason(entity.getCancelReason());
        return info;
    }
}
