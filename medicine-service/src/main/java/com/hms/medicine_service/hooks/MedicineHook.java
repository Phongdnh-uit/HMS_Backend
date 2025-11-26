package com.hms.medicine_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.medicine_service.dtos.medicine.MedicineRequest;
import com.hms.medicine_service.dtos.medicine.MedicineResponse;
import com.hms.medicine_service.entities.Medicine;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MedicineHook implements GenericHook<Medicine, String, MedicineRequest, MedicineResponse> {
    @Override
    public void enrichFindAll(PageResponse<MedicineResponse> response) {

    }

    @Override
    public void enrichFindById(MedicineResponse response) {

    }

    @Override
    public void validateCreate(MedicineRequest input, Map<String, Object> context) {

    }

    @Override
    public void enrichCreate(MedicineRequest input, Medicine entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Medicine entity, MedicineResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, MedicineRequest input, Medicine existingEntity, Map<String, Object> context) {

    }

    @Override
    public void enrichUpdate(MedicineRequest input, Medicine entity, Map<String, Object> context) {

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
}
