package com.hms.patient_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.patient_service.dtos.category.CategoryRequest;
import com.hms.patient_service.dtos.category.CategoryResponse;
import com.hms.patient_service.entities.Category;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper extends GenericMapper<Category, CategoryRequest, CategoryResponse> {
}
