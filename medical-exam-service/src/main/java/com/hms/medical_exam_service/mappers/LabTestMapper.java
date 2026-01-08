package com.hms.medical_exam_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.medical_exam_service.dtos.lab.LabTestRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResponse;
import com.hms.medical_exam_service.entities.LabTest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabTestMapper extends GenericMapper<LabTest, LabTestRequest, LabTestResponse> {

    @Override
    LabTest requestToEntity(LabTestRequest request);

    @Override
    LabTestResponse entityToResponse(LabTest entity);

    @Override
    void partialUpdate(LabTestRequest request, @MappingTarget LabTest entity);
}
