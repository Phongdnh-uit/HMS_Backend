package com.hms.patient_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.entities.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PatientMapper extends GenericMapper<Patient, PatientRequest, PatientResponse> {
}
