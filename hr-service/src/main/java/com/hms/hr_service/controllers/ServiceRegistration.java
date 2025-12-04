package com.hms.hr_service.controllers;

import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.hooks.DepartmentHook;
import com.hms.hr_service.mappers.DepartmentMapper;
import com.hms.hr_service.repositories.DepartmentRepository;
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
}