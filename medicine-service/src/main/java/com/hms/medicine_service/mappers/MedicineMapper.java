package com.hms.medicine_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.medicine_service.dtos.medicine.MedicineRequest;
import com.hms.medicine_service.dtos.medicine.MedicineResponse;
import com.hms.medicine_service.entities.Medicine;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicineMapper extends GenericMapper<Medicine, MedicineRequest, MedicineResponse> {
    @org.mapstruct.AfterMapping
    default void mapCategory(Medicine entity, @org.mapstruct.MappingTarget MedicineResponse response) {
        if (entity.getCategory() != null) {
            response.setCategoryId(entity.getCategory().getId());
            response.setCategoryName(entity.getCategory().getName());
        }
    }
}
