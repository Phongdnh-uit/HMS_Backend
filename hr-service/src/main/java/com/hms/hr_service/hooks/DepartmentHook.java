package com.hms.hr_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.entities.Department;
import org.springframework.stereotype.Component;

import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DepartmentHook implements GenericHook<Department, String, DepartmentRequest, DepartmentResponse> {
    private final EmployeeRepository employeeRepository;

    @Override
    public void enrichFindAll(PageResponse<DepartmentResponse> response) {
        List<String> doctorIds = response.getContent().stream()
                .map(DepartmentResponse::getHeadDoctorId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (!doctorIds.isEmpty()) {
            Map<String, String> doctorMap = employeeRepository.findAllById(doctorIds).stream()
                    .collect(Collectors.toMap(Employee::getId, Employee::getFullName));

            response.getContent().forEach(d -> {
                if (d.getHeadDoctorId() != null) {
                    d.setHeadDoctorName(doctorMap.get(d.getHeadDoctorId()));
                }
            });
        }
    }

    @Override
    public void enrichFindById(DepartmentResponse response) {
        if (response.getHeadDoctorId() != null) {
            employeeRepository.findById(response.getHeadDoctorId())
                    .ifPresent(e -> response.setHeadDoctorName(e.getFullName()));
        }
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
