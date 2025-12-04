package com.hms.hr_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper extends GenericMapper<Employee, EmployeeRequest, EmployeeResponse> {
}
