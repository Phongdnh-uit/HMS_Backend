package com.hms.hr_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.entities.Department;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DepartmentHook implements GenericHook<Department, String, DepartmentRequest, DepartmentResponse> {
    @Override
    public void enrichFindAll(PageResponse<DepartmentResponse> response) {

    }

    @Override
    public void enrichFindById(DepartmentResponse response) {

    }

    @Override
    public void validateCreate(DepartmentRequest input, Map<String, Object> context) {

    }

    @Override
    public void enrichCreate(DepartmentRequest input, Department entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Department entity, DepartmentResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, DepartmentRequest input, Department existingEntity, Map<String, Object> context) {

    }

    @Override
    public void enrichUpdate(DepartmentRequest input, Department entity, Map<String, Object> context) {

    }

    @Override
    public void afterUpdate(Department entity, DepartmentResponse response, Map<String, Object> context) {

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
