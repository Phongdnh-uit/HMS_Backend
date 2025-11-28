package com.hms.patient_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.patient_service.entities.Category;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends SimpleRepository<Category, String> {
}
