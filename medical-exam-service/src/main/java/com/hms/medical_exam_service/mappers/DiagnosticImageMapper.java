package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.lab.DiagnosticImageResponse;
import com.hms.medical_exam_service.entities.DiagnosticImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiagnosticImageMapper {

    @Mapping(target = "downloadUrl", ignore = true) // Populated by service
    @Mapping(target = "thumbnailUrl", ignore = true) // Populated by service
    DiagnosticImageResponse entityToResponse(DiagnosticImage entity);

    List<DiagnosticImageResponse> entitiesToResponses(List<DiagnosticImage> entities);
}
