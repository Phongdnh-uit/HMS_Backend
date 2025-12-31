package com.hms.hr_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.hr_service.entities.Employee;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends SimpleRepository<Employee, String> {
    Optional<Employee> findByAccountId(String accountId);
}
