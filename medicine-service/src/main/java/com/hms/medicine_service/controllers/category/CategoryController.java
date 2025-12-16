package com.hms.medicine_service.controllers.category;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.services.CrudService;
import com.hms.medicine_service.dtos.category.CategoryRequest;
import com.hms.medicine_service.dtos.category.CategoryResponse;
import com.hms.medicine_service.entities.Category;
import io.github.perplexhub.rsql.RSQLJPASupport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/medicines/categories")
@RestController
public class CategoryController extends GenericController<Category, String, CategoryRequest, CategoryResponse> {
    public CategoryController(CrudService<Category, String, CategoryRequest, CategoryResponse> service) {
        super(service);
    }

    /**
     * Override to match API contract: GET /api/medicines/categories (instead of /all)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> listCategories(
            Pageable pageable,
            @RequestParam(value = "search", required = false) @Nullable String search) {
        Specification<Category> specification = RSQLJPASupport.toSpecification(search);
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }
}
