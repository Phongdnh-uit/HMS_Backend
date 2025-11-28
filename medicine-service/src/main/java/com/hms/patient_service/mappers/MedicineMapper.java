package com.hms.patient_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.patient_service.dtos.medicine.MedicineRequest;
import com.hms.patient_service.dtos.medicine.MedicineResponse;
import com.hms.patient_service.entities.Medicine;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicineMapper extends GenericMapper<Medicine, MedicineRequest, MedicineResponse> {
}
