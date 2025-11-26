package com.hms.medicine_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.medicine_service.dtos.category.CategoryRequest;
import com.hms.medicine_service.dtos.category.CategoryResponse;
import com.hms.medicine_service.entities.Category;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CategoryHook implements GenericHook<Category, String, CategoryRequest, CategoryResponse> {
    @Override
    public void enrichFindAll(PageResponse<CategoryResponse> response) {

    }

    @Override
    public void enrichFindById(CategoryResponse response) {

    }

    @Override
    public void validateCreate(CategoryRequest input, Map<String, Object> context) {

    }

    @Override
    public void enrichCreate(CategoryRequest input, Category entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Category entity, CategoryResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, CategoryRequest input, Category existingEntity, Map<String, Object> context) {

    }

    @Override
    public void enrichUpdate(CategoryRequest input, Category entity, Map<String, Object> context) {

    }

    @Override
    public void afterUpdate(Category entity, CategoryResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateDelete(String s) {

    }

    @Override
    public void afterDelete(String s) {

    }

    @Override
    public void validateBulkDelete(Iterable<String> strings) {

    }

    @Override
    public void afterBulkDelete(Iterable<String> strings) {

    }
}
