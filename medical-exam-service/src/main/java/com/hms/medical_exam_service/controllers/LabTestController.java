package com.hms.medical_exam_service.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.medical_exam_service.dtos.lab.LabTestRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResponse;
import com.hms.medical_exam_service.entities.LabTest;
import com.hms.medical_exam_service.entities.LabTestCategory;
import com.hms.medical_exam_service.mappers.LabTestMapper;
import com.hms.medical_exam_service.repositories.LabTestRepository;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Lab Test type management (CRUD).
 * 
 * Endpoints:
 * - GET /exams/lab-tests/all - List all lab tests (with filter, pagination)
 * - GET /exams/lab-tests/active - List active lab tests only
 * - GET /exams/lab-tests/category/{category} - List by category
 * - GET /exams/lab-tests/{id} - Get by ID
 * - POST /exams/lab-tests - Create (ADMIN only)
 * - PUT /exams/lab-tests/{id} - Update (ADMIN only)
 * - DELETE /exams/lab-tests/{id} - Deactivate (ADMIN only)
 */
@RestController
@RequestMapping("/exams/lab-tests")
@RequiredArgsConstructor
@Slf4j
public class LabTestController {
    
    private final LabTestRepository repository;
    private final LabTestMapper mapper;
    
    /**
     * Get all lab tests with pagination and filtering
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<LabTestResponse>>> findAll(
            Pageable pageable,
            @RequestParam(value = "filter", required = false) @Nullable String filter,
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        
        log.info("GET /exams/lab-tests/all - filter: {}, all: {}", filter, all);
        
        Specification<LabTest> specification = RSQLJPASupport.toSpecification(filter);
        if (all) {
            pageable = Pageable.unpaged(pageable.getSort());
        }
        
        Page<LabTest> page = repository.findAll(specification, pageable);
        Page<LabTestResponse> responsePage = page.map(mapper::entityToResponse);
        
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.fromPage(responsePage)));
    }
    
    /**
     * Get lab test by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabTestResponse>> findById(@PathVariable String id) {
        return repository.findById(id)
                .map(mapper::entityToResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.ok(response)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all active lab tests (for dropdown menus)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LabTestResponse>>> getActive() {
        List<LabTestResponse> tests = repository.findByIsActiveTrue()
                .stream()
                .map(mapper::entityToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(tests));
    }
    
    /**
     * Get lab tests by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<LabTestResponse>>> getByCategory(
            @PathVariable LabTestCategory category) {
        List<LabTestResponse> tests = repository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(mapper::entityToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(tests));
    }
    
    /**
     * Get lab test by code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<LabTestResponse>> getByCode(@PathVariable String code) {
        return repository.findByCode(code)
                .map(mapper::entityToResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.ok(response)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Create new lab test
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LabTestResponse>> create(@Valid @RequestBody LabTestRequest request) {
        log.info("POST /exams/lab-tests - Creating: {}", request.getCode());
        
        LabTest entity = mapper.requestToEntity(request);
        entity = repository.save(entity);
        
        return ResponseEntity.ok(ApiResponse.ok(mapper.entityToResponse(entity)));
    }
    
    /**
     * Update lab test
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LabTestResponse>> update(
            @PathVariable String id, 
            @Valid @RequestBody LabTestRequest request) {
        
        return repository.findById(id)
                .map(entity -> {
                    mapper.partialUpdate(request, entity);
                    entity = repository.save(entity);
                    return ResponseEntity.ok(ApiResponse.ok(mapper.entityToResponse(entity)));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Delete lab test (soft delete - set isActive = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        return repository.findById(id)
                .map(entity -> {
                    entity.setIsActive(false);
                    repository.save(entity);
                    return ResponseEntity.ok(ApiResponse.<Void>ok(null));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
