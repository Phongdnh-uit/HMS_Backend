package com.hms.patient_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.patient_service.dtos.category.CategoryRequest;
import com.hms.patient_service.dtos.category.CategoryResponse;
import com.hms.patient_service.entities.Category;
import com.hms.patient_service.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CategoryHook implements GenericHook<Category, String, CategoryRequest, CategoryResponse> {
    private final CategoryRepository categoryRepository;

    @Override
    public void enrichFindAll(PageResponse<CategoryResponse> response) {

    }

    @Override
    public void enrichFindById(CategoryResponse response) {

    }

    @Override
    public void validateCreate(CategoryRequest input, Map<String, Object> context) {
        validate(input, null);
    }

    @Override
    public void enrichCreate(CategoryRequest input, Category entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Category entity, CategoryResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String id, CategoryRequest input, Category existingEntity, Map<String, Object> context) {
        validate(input, id);
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

    private void validate(CategoryRequest request, String id) {
        Specification<Category> nameSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("name")), request.getName().toLowerCase());
        if (id != null) {
            nameSpec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.notEqual(root.get("id"), id));
        }
        if (categoryRepository.count(nameSpec) > 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, Map.of(
                    "name", "Category with name '" + request.getName() + "' already exists"
            ));
        }
    }
}
