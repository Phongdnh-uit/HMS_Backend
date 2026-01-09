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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import com.hms.hr_service.entities.Department;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class EmployeeHook implements GenericHook<Employee, String, EmployeeRequest, EmployeeResponse> {
    private final DepartmentRepository departmentRepository;
    private final AccountClient accountClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;


    @Override
    public void enrichFindAll(PageResponse<EmployeeResponse> response) {
        List<String> departmentIds = response.getContent().stream()
                .map(EmployeeResponse::getDepartmentId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!departmentIds.isEmpty()) {
            Map<String, String> departmentMap = departmentRepository.findAllById(departmentIds).stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName));

            response.getContent().forEach(e -> {
                if (e.getDepartmentId() != null) {
                    e.setDepartmentName(departmentMap.get(e.getDepartmentId()));
                }
            });
        }
    }

    @Override
    public void enrichFindById(EmployeeResponse response) {
        if (response.getDepartmentId() != null) {
            departmentRepository.findById(response.getDepartmentId())
                    .ifPresent(d -> response.setDepartmentName(d.getName()));
        }
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

        // validate accountId if provided (with Circuit Breaker)
        if (request.getAccountId() != null && !request.getAccountId().isBlank()) {
            var authCircuitBreaker = circuitBreakerFactory.create("hrAuth");
            var response = authCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> accountClient.findById(request.getAccountId())),
                throwable -> {
                    log.warn("[CB-FALLBACK] Auth service unavailable: {}", throwable.getMessage());
                    // Cannot validate account - fail safe by blocking
                    throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                        "Auth service unavailable. Cannot validate account ID.");
                }
            );
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
