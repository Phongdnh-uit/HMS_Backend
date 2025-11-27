package com.hms.medicine_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medicine_service.entities.Category;
import com.hms.medicine_service.entities.Medicine;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends SimpleRepository<Category, String> {
}
