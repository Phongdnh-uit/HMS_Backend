package com.hms.hr_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Employee;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmployeeHook implements GenericHook<Employee, String, EmployeeRequest, EmployeeResponse> {
    @Override
    public void enrichFindAll(PageResponse<EmployeeResponse> response) {

    }

    @Override
    public void enrichFindById(EmployeeResponse response) {

    }

    @Override
    public void validateCreate(EmployeeRequest input, Map<String, Object> context) {

    }

    @Override
    public void enrichCreate(EmployeeRequest input, Employee entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Employee entity, EmployeeResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, EmployeeRequest input, Employee existingEntity, Map<String, Object> context) {

    }

    @Override
    public void enrichUpdate(EmployeeRequest input, Employee entity, Map<String, Object> context) {

    }

    @Override
    public void afterUpdate(Employee entity, EmployeeResponse response, Map<String, Object> context) {

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
