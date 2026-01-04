package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.lab.LabTestResultRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResultResponse;
import com.hms.medical_exam_service.dtos.lab.LabTestResultUpdateRequest;
import com.hms.medical_exam_service.entities.LabTestResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabTestResultMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "labTestCode", ignore = true)
    @Mapping(target = "labTestName", ignore = true)
    @Mapping(target = "labTestCategory", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "performedAt", ignore = true)
    @Mapping(target = "interpretedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LabTestResult requestToEntity(LabTestResultRequest request);

    @Mapping(target = "images", ignore = true) // Images populated separately
    LabTestResultResponse entityToResponse(LabTestResult entity);

    @Mapping(target = "medicalExamId", ignore = true)
    @Mapping(target = "labTestId", ignore = true)
    void updateFromRequest(LabTestResultUpdateRequest request, @MappingTarget LabTestResult entity);
}
