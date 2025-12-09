package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemResponse;
import com.hms.medical_exam_service.entities.PrescriptionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper for PrescriptionItem.
 * Note: Does NOT extend GenericMapper because PrescriptionItem is a child entity
 * managed through Prescription (cascade), not independently via CRUD endpoints.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionItemMapper {

    /**
     * Maps request to entity.
     * Note: medicineName and unitPrice are NOT mapped here - they are set in hook
     * after fetching from medicine-service (price snapshot pattern).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "prescription", ignore = true)    // Set by parent entity
    @Mapping(target = "medicineName", ignore = true)    // Set in hook from medicine-service
    @Mapping(target = "unitPrice", ignore = true)       // Set in hook from medicine-service (snapshot)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PrescriptionItem requestToEntity(PrescriptionItemRequest request);

    /**
     * Maps entity to response with nested medicine info
     */
    @Mapping(target = "medicine", expression = "java(mapMedicineInfo(entity))")
    PrescriptionItemResponse entityToResponse(PrescriptionItem entity);

    /**
     * Maps list of requests to list of entities
     */
    List<PrescriptionItem> requestsToEntities(List<PrescriptionItemRequest> requests);

    /**
     * Maps list of entities to list of responses
     */
    List<PrescriptionItemResponse> entitiesToResponses(List<PrescriptionItem> entities);

    /**
     * Maps entity fields to nested MedicineInfo object (snapshot data)
     */
    default PrescriptionItemResponse.MedicineInfo mapMedicineInfo(PrescriptionItem entity) {
        if (entity == null) {
            return null;
        }
        
        PrescriptionItemResponse.MedicineInfo medicineInfo = new PrescriptionItemResponse.MedicineInfo();
        medicineInfo.setId(entity.getMedicineId());
        medicineInfo.setName(entity.getMedicineName());
        return medicineInfo;
    }
}
