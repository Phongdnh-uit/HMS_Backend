package com.hms.medical_exam_service.services;

import com.hms.medical_exam_service.dtos.lab.LabTestRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResponse;
import com.hms.medical_exam_service.entities.LabTest;
import com.hms.medical_exam_service.entities.LabTestCategory;
import com.hms.medical_exam_service.mappers.LabTestMapper;
import com.hms.medical_exam_service.repositories.LabTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Lab Test (catalog) management.
 * Standalone implementation without GenericService inheritance.
 */
@Service
@RequiredArgsConstructor
public class LabTestService {
    
    private final LabTestRepository repository;
    private final LabTestMapper mapper;
    
    /**
     * Find lab test by ID
     */
    public Optional<LabTest> findById(String id) {
        return repository.findById(id);
    }
    
    /**
     * Find lab test by code
     */
    public LabTestResponse findByCode(String code) {
        return repository.findByCode(code)
                .map(mapper::entityToResponse)
                .orElse(null);
    }
    
    /**
     * Get all active lab tests
     */
    public List<LabTestResponse> findAllActive() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(mapper::entityToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get lab tests by category
     */
    public List<LabTestResponse> findByCategory(LabTestCategory category) {
        return repository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(mapper::entityToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if code exists (for validation)
     */
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }
}
