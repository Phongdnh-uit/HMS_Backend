package com.hms.hr_service.hooks;

import com.hms.common.clients.AccountClient;
import com.hms.common.dtos.PageResponse;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.helpers.FeignHelper;
import com.hms.common.hooks.GenericHook;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class EmployeeHook implements GenericHook<Employee, String, EmployeeRequest, EmployeeResponse> {
    private final DepartmentRepository departmentRepository;
    private final AccountClient accountClient;


    @Override
    public void enrichFindAll(PageResponse<EmployeeResponse> response) {

    }

    @Override
    public void enrichFindById(EmployeeResponse response) {

    }

    @Override
    public void validateCreate(EmployeeRequest input, Map<String, Object> context) {
        validate(input);
    }

    @Override
    public void enrichCreate(EmployeeRequest input, Employee entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Employee entity, EmployeeResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, EmployeeRequest input, Employee existingEntity, Map<String, Object> context) {
        validate(input);
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

    private void validate(EmployeeRequest request) {
        Map<String, String> errors = new java.util.HashMap<>();

        // validate accountId if provided
        if (!request.getAccountId().isBlank()) {
            var response = FeignHelper.safeCall(() -> accountClient.findById(request.getAccountId()));
            if (response.getCode() != 1000) {
                errors.put("accountId", "Account with ID " + request.getAccountId() + " does not exist");
            }
        }

        if (!departmentRepository.existsById(request.getDepartmentId())) {
            errors.put("departmentId", "Department with ID " + request.getDepartmentId() + " does not exist");
        }

        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
        }
    }
}
