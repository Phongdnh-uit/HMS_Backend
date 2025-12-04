package com.hms.hr_service.controllers;

import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.hooks.DepartmentHook;
import com.hms.hr_service.hooks.EmployeeHook;
import com.hms.hr_service.mappers.DepartmentMapper;
import com.hms.hr_service.mappers.EmployeeMapper;
import com.hms.hr_service.repositories.DepartmentRepository;
import com.hms.hr_service.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {
    private final ApplicationContext context;

    @Bean
    CrudService<Department, String, DepartmentRequest, DepartmentResponse> departmentService() {
        return new GenericService<Department, String, DepartmentRequest, DepartmentResponse>(
                context.getBean(DepartmentRepository.class),
                context.getBean(DepartmentMapper.class),
                context.getBean(DepartmentHook.class)
        );
    }

    @Bean
    CrudService<Employee, String, EmployeeRequest, EmployeeResponse> employeeService() {
        return new GenericService<Employee, String, EmployeeRequest, EmployeeResponse>(
                context.getBean(EmployeeRepository.class),
                context.getBean(EmployeeMapper.class),
                context.getBean(EmployeeHook.class)
        );
    }
}