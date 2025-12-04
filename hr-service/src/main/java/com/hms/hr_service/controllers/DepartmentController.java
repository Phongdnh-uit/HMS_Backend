package com.hms.hr_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.services.CrudService;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.entities.Department;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/hr/departments")
@RestController
public class DepartmentController extends GenericController<Department, String, DepartmentRequest, DepartmentResponse> {
    public DepartmentController(CrudService<Department, String, DepartmentRequest, DepartmentResponse> service) {
        super(service);
    }
}
