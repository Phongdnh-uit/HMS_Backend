package com.hms.medical_exam_service.services;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.medical_exam_service.dtos.lab.*;
import com.hms.medical_exam_service.entities.*;
import com.hms.medical_exam_service.mappers.DiagnosticImageMapper;
import com.hms.medical_exam_service.mappers.LabTestResultMapper;
import com.hms.medical_exam_service.repositories.DiagnosticImageRepository;
import com.hms.medical_exam_service.repositories.LabTestRepository;
import com.hms.medical_exam_service.repositories.LabTestResultRepository;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabTestResultService {
    
    private final LabTestResultRepository resultRepository;
    private final LabTestRepository labTestRepository;
    private final MedicalExamRepository medicalExamRepository;
    private final DiagnosticImageRepository imageRepository;
    private final LabTestResultMapper resultMapper;
    private final DiagnosticImageMapper imageMapper;
    private final FileStorageService fileStorageService;
    
    /**
     * Create a new lab test result (order a test)
     */
    @Transactional
    public LabTestResultResponse create(LabTestResultRequest request) {
        // Validate medical exam exists
        MedicalExam exam = medicalExamRepository.findById(request.getMedicalExamId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Medical exam not found"));
        
        // Validate lab test exists
        LabTest labTest = labTestRepository.findById(request.getLabTestId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab test not found"));
        
        // Create entity
        LabTestResult result = resultMapper.requestToEntity(request);
        result.setStatus(ResultStatus.PENDING);
        
        // Set default for isAbnormal to prevent null constraint violation
        if (result.getIsAbnormal() == null) {
            result.setIsAbnormal(false);
        }
        
        // Copy denormalized data
        result.setPatientId(exam.getPatientId());
        result.setPatientName(exam.getPatientName());
        result.setLabTestCode(labTest.getCode());
        result.setLabTestName(labTest.getName());
        result.setLabTestCategory(labTest.getCategory());
        result.setLabTestPrice(labTest.getPrice());
        
        result = resultRepository.save(result);
        
        return toResponse(result);
    }
    
    /**
     * Get lab test result by ID with images
     */
    public LabTestResultResponse findById(String id) {
        LabTestResult result = resultRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab test result not found"));
        return toResponse(result);
    }
    
    /**
     * Get all results for a medical exam
     */
    public List<LabTestResultResponse> findByMedicalExam(String medicalExamId) {
        return resultRepository.findByMedicalExamId(medicalExamId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all results for a patient
     */
    public List<LabTestResultResponse> findByPatient(String patientId) {
        return resultRepository.findByPatientId(patientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get paginated results with filtering
     */
    public Page<LabTestResultResponse> findAll(Pageable pageable) {
        return resultRepository.findAll(pageable)
                .map(this::toResponse);
    }
    
    /**
     * Update lab test result
     */
    @Transactional
    public LabTestResultResponse update(String id, LabTestResultUpdateRequest request) {
        LabTestResult result = resultRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab test result not found"));
        
        resultMapper.updateFromRequest(request, result);
        
        // Handle status transitions
        if (request.getStatus() != null) {
            if (request.getStatus() == ResultStatus.PROCESSING && result.getPerformedAt() == null) {
                result.setPerformedAt(Instant.now());
            }
            if (request.getStatus() == ResultStatus.COMPLETED && result.getCompletedAt() == null) {
                result.setCompletedAt(Instant.now());
            }
        }
        
        result = resultRepository.save(result);
        return toResponse(result);
    }
    
    /**
     * Upload images for a lab test result
     */
    @Transactional
    public List<DiagnosticImageResponse> uploadImages(String resultId, List<MultipartFile> files, 
                                                       ImageType imageType, String description, String uploadedBy) {
        LabTestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab test result not found"));
        
        List<DiagnosticImage> savedImages = new ArrayList<>();
        int sequenceNumber = (int) imageRepository.countByLabTestResultId(resultId);
        
        for (MultipartFile file : files) {
            sequenceNumber++;
            
            // Upload to MinIO
            String storagePath = fileStorageService.uploadFile(file, result.getPatientId(), resultId);
            
            // Create image record
            DiagnosticImage image = new DiagnosticImage();
            image.setLabTestResultId(resultId);
            image.setFileName(file.getOriginalFilename());
            image.setStoragePath(storagePath);
            image.setContentType(file.getContentType());
            image.setFileSize(file.getSize());
            image.setImageType(imageType);
            image.setDescription(description);
            image.setSequenceNumber(sequenceNumber);
            image.setUploadedBy(uploadedBy);
            
            savedImages.add(imageRepository.save(image));
        }
        
        return savedImages.stream()
                .map(this::toImageResponseWithUrl)
                .collect(Collectors.toList());
    }
    
    /**
     * Get images for a lab test result
     */
    public List<DiagnosticImageResponse> getImages(String resultId) {
        return imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(resultId)
                .stream()
                .map(this::toImageResponseWithUrl)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete an image
     */
    @Transactional
    public void deleteImage(String imageId) {
        DiagnosticImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Image not found"));
        
        // Delete from MinIO
        fileStorageService.deleteFile(image.getStoragePath());
        
        // Delete record
        imageRepository.delete(image);
    }
    
    /**
     * Convert entity to response with images
     */
    private LabTestResultResponse toResponse(LabTestResult result) {
        LabTestResultResponse response = resultMapper.entityToResponse(result);
        
        // Load images
        List<DiagnosticImage> images = imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(result.getId());
        response.setImages(images.stream()
                .map(this::toImageResponseWithUrl)
                .collect(Collectors.toList()));
        
        return response;
    }
    
    /**
     * Convert image entity to response with download URLs
     */
    private DiagnosticImageResponse toImageResponseWithUrl(DiagnosticImage image) {
        DiagnosticImageResponse response = imageMapper.entityToResponse(image);
        
        // Use full URL with API gateway host so browser can access directly
        // Format: http://localhost:8080/api/exams/lab-results/images/{imageId}/download
        String baseUrl = "http://localhost:8080";
        response.setDownloadUrl(baseUrl + "/api/exams/lab-results/images/" + image.getId() + "/download");
        if (image.getThumbnailPath() != null) {
            response.setThumbnailUrl(baseUrl + "/api/exams/lab-results/images/" + image.getId() + "/download");
        }
        
        return response;
    }
    
    /**
     * Download image as bytes for proxy endpoint
     */
    public ResponseEntity<byte[]> downloadImageAsBytes(String imageId) {
        DiagnosticImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Image not found"));
        
        try (InputStream is = fileStorageService.downloadFile(image.getStoragePath())) {
            byte[] bytes = is.readAllBytes();
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    image.getContentType() != null ? image.getContentType() : "application/octet-stream"));
            headers.setContentLength(bytes.length);
            headers.setContentDispositionFormData("inline", image.getFileName());
            
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            log.error("Failed to download image {}: {}", imageId, e.getMessage());
            throw new RuntimeException("Failed to download image: " + e.getMessage(), e);
        }
    }
}

