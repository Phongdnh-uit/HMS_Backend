package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.lab.LabOrderResponse;
import com.hms.medical_exam_service.dtos.lab.LabTestResultResponse;
import com.hms.medical_exam_service.entities.LabOrder;
import com.hms.medical_exam_service.entities.LabTestResult;
import com.hms.medical_exam_service.entities.ResultStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {LabTestResultMapper.class})
public interface LabOrderMapper {

    @Mapping(target = "totalTests", expression = "java(entity.getResults() != null ? entity.getResults().size() : 0)")
    @Mapping(target = "completedTests", expression = "java(countCompletedTests(entity))")
    @Mapping(target = "pendingTests", expression = "java(countPendingTests(entity))")
    @Mapping(target = "results", source = "results")
    LabOrderResponse entityToResponse(LabOrder entity);

    List<LabOrderResponse> entitiesToResponses(List<LabOrder> entities);

    default int countCompletedTests(LabOrder entity) {
        if (entity.getResults() == null) return 0;
        return (int) entity.getResults().stream()
                .filter(r -> r.getStatus() == ResultStatus.COMPLETED)
                .count();
    }

    default int countPendingTests(LabOrder entity) {
        if (entity.getResults() == null) return 0;
        return (int) entity.getResults().stream()
                .filter(r -> r.getStatus() == ResultStatus.PENDING || r.getStatus() == ResultStatus.PROCESSING)
                .count();
    }
}
