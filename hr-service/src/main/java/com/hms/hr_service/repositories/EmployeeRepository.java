package com.hms.hr_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.hr_service.entities.Employee;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends SimpleRepository<Employee, String> {
}
