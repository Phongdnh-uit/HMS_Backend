package com.hms.hr_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.hr_service.entities.Department;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends SimpleRepository<Department, String> {
}
