package com.hms.patient_service.controllers.category;

import com.hms.common.controllers.GenericController;
import com.hms.common.services.CrudService;
import com.hms.patient_service.dtos.category.CategoryRequest;
import com.hms.patient_service.dtos.category.CategoryResponse;
import com.hms.patient_service.entities.Category;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/categories")
@RestController
public class CategoryController extends GenericController<Category, String, CategoryRequest, CategoryResponse> {
    public CategoryController(CrudService<Category, String, CategoryRequest, CategoryResponse> service) {
        super(service);
    }
}
