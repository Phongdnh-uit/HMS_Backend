package com.hms.medical_exam_service.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.medical_exam_service.dtos.lab.*;
import com.hms.medical_exam_service.entities.ImageType;
import com.hms.medical_exam_service.services.LabTestResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for Lab Test Result management.
 * 
 * Endpoints:
 * - GET /exams/lab-results/all - List all results (with pagination)
 * - GET /exams/lab-results/{id} - Get result by ID with images
 * - GET /exams/lab-results/exam/{examId} - Get results for a medical exam
 * - GET /exams/lab-results/patient/{patientId} - Get results for a patient
 * - POST /exams/lab-results - Order a new lab test
 * - PUT /exams/lab-results/{id} - Update result
 * - POST /exams/lab-results/{id}/images - Upload images
 * - GET /exams/lab-results/{id}/images - Get images list
 * - DELETE /exams/images/{imageId} - Delete an image
 */
@RestController
@RequestMapping("/exams/lab-results")
@RequiredArgsConstructor
@Slf4j
public class LabTestResultController {
    
    private final LabTestResultService service;
    
    /**
     * List all lab test results with pagination
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<LabTestResultResponse>>> findAll(Pageable pageable) {
        Page<LabTestResultResponse> page = service.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.fromPage(page)));
    }
    
    /**
     * Get lab test result by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabTestResultResponse>> findById(@PathVariable String id) {
        LabTestResultResponse response = service.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    /**
     * Get all results for a medical exam
     */
    @GetMapping("/exam/{examId}")
    public ResponseEntity<ApiResponse<List<LabTestResultResponse>>> findByExam(@PathVariable String examId) {
        List<LabTestResultResponse> results = service.findByMedicalExam(examId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
    
    /**
     * Get all results for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<LabTestResultResponse>>> findByPatient(@PathVariable String patientId) {
        List<LabTestResultResponse> results = service.findByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
    
    /**
     * Order a new lab test (create result in PENDING status)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LabTestResultResponse>> create(
            @Valid @RequestBody LabTestResultRequest request) {
        LabTestResultResponse result = service.create(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    /**
     * Update lab test result
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LabTestResultResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody LabTestResultUpdateRequest request) {
        LabTestResultResponse result = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    /**
     * Upload images for a lab test result
     */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<DiagnosticImageResponse>>> uploadImages(
            @PathVariable String id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "imageType", defaultValue = "PHOTO") ImageType imageType,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "X-User-ID", required = false) String userId) {
        
        List<DiagnosticImageResponse> images = service.uploadImages(id, files, imageType, description, userId);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }
    
    /**
     * Get images for a lab test result
     */
    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<DiagnosticImageResponse>>> getImages(@PathVariable String id) {
        List<DiagnosticImageResponse> images = service.getImages(id);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }
    
    /**
     * Delete an image
     */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String imageId) {
        service.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    
    /**
     * Proxy download endpoint - streams file from MinIO through API Gateway
     * This avoids the need for browser to access MinIO directly
     */
    @GetMapping("/images/{imageId}/download")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String imageId) {
        return service.downloadImageAsBytes(imageId);
    }
}
