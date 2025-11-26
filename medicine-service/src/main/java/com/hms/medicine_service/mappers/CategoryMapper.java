package com.hms.medicine_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.medicine_service.dtos.category.CategoryRequest;
import com.hms.medicine_service.dtos.category.CategoryResponse;
import com.hms.medicine_service.entities.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {})
public interface CategoryMapper extends GenericMapper<Category, CategoryRequest, CategoryResponse> {
}
