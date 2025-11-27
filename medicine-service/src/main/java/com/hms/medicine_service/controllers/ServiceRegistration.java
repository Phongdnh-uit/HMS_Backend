package com.hms.medicine_service.controllers;

import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import com.hms.medicine_service.dtos.category.CategoryRequest;
import com.hms.medicine_service.dtos.category.CategoryResponse;
import com.hms.medicine_service.dtos.medicine.MedicineRequest;
import com.hms.medicine_service.dtos.medicine.MedicineResponse;
import com.hms.medicine_service.entities.Category;
import com.hms.medicine_service.entities.Medicine;
import com.hms.medicine_service.hooks.CategoryHook;
import com.hms.medicine_service.hooks.MedicineHook;
import com.hms.medicine_service.mappers.CategoryMapper;
import com.hms.medicine_service.mappers.MedicineMapper;
import com.hms.medicine_service.repositories.CategoryRepository;
import com.hms.medicine_service.repositories.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {

    private final ApplicationContext context;

    @Bean
    CrudService<Medicine, String, MedicineRequest, MedicineResponse> medicineService() {
        return new GenericService<Medicine, String, MedicineRequest, MedicineResponse>(
                context.getBean(MedicineRepository.class),
                context.getBean(MedicineMapper.class),
                context.getBean(MedicineHook.class)
        );
    }

    @Bean
    CrudService<Category, String, CategoryRequest, CategoryResponse> categoryService() {
        return new GenericService<Category, String, CategoryRequest, CategoryResponse>(
                context.getBean(CategoryRepository.class),
                context.getBean(CategoryMapper.class),
                context.getBean(CategoryHook.class)
        );
    }
}
