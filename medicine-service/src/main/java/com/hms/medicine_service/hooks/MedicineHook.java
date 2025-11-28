package com.hms.patient_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.patient_service.dtos.medicine.MedicineRequest;
import com.hms.patient_service.dtos.medicine.MedicineResponse;
import com.hms.patient_service.entities.Medicine;
import com.hms.patient_service.repositories.CategoryRepository;
import com.hms.patient_service.repositories.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class MedicineHook implements GenericHook<Medicine, String, MedicineRequest, MedicineResponse> {

    private final MedicineRepository medicineRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void enrichFindAll(PageResponse<MedicineResponse> response) {

    }

    @Override
    public void enrichFindById(MedicineResponse response) {

    }

    @Override
    public void validateCreate(MedicineRequest input, Map<String, Object> context) {
        validate(input, null);
    }

    @Override
    public void enrichCreate(MedicineRequest input, Medicine entity, Map<String, Object> context) {
        enrich(input, entity);
    }

    @Override
    public void afterCreate(Medicine entity, MedicineResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, MedicineRequest input, Medicine existingEntity, Map<String, Object> context) {
        validate(input, s);
    }

    @Override
    public void enrichUpdate(MedicineRequest input, Medicine entity, Map<String, Object> context) {
        enrich(input, entity);
    }

    @Override
    public void afterUpdate(Medicine entity, MedicineResponse response, Map<String, Object> context) {

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

    private void validate(MedicineRequest request, String id) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, Map.of("categoryId", "Category with id " + request.getCategoryId() + " does not exist"));
        }
    }

    private void enrich(MedicineRequest request, Medicine medicine) {
        medicine.setCategory(categoryRepository.getReferenceById(request.getCategoryId()));
    }
}
