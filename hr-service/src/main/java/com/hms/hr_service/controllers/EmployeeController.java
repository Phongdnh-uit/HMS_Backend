package com.hms.hr_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.services.CrudService;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Employee;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/hr/employees")
@RestController
public class EmployeeController extends GenericController<Employee, String, EmployeeRequest, EmployeeResponse> {
    public EmployeeController(CrudService<Employee, String, EmployeeRequest, EmployeeResponse> service) {
        super(service);
    }
}
