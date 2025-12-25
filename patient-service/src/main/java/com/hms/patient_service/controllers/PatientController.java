package com.hms.patient_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.securities.UserContext;
import com.hms.common.services.CrudService;
import com.hms.patient_service.dtos.patient.PatientStatsResponse;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.dtos.patient.PatientSelfUpdateRequest;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.mappers.PatientMapper;
import com.hms.patient_service.repositories.PatientRepository;
import com.hms.patient_service.services.FileStorageService;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/patients")
@RestController
public class PatientController extends GenericController<Patient, String, PatientRequest, PatientResponse> {
    
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final FileStorageService fileStorageService;

    public PatientController(
            CrudService<Patient, String, PatientRequest, PatientResponse> service,
            PatientRepository patientRepository,
            PatientMapper patientMapper,
            FileStorageService fileStorageService) {
        super(service);
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Override to match API contract: GET /api/patients (instead of /api/patients/all)
     * Supports query params: page, size, search (RSQL), sort
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PatientResponse>>> listPatients(
            Pageable pageable,
            @RequestParam(value = "search", required = false) @Nullable String search) {
        Specification<Patient> specification = RSQLJPASupport.toSpecification(search);
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile() {
        UserContext.User currentUser = UserContext.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED, "User not authenticated");
        }
        
        Patient patient = patientRepository.findByAccountId(currentUser.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Patient profile not found"));
        
        PatientResponse response = patientMapper.entityToResponse(patient);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<PatientResponse>> updateMyProfile(
            @Valid @RequestBody PatientSelfUpdateRequest request) {
        UserContext.User currentUser = UserContext.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED, "User not authenticated");
        }
        
        Patient patient = patientRepository.findByAccountId(currentUser.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Patient profile not found"));
        
        // Only update non-null fields (partial update)
        if (request.getPhoneNumber() != null) {
            patient.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress());
        }
        if (request.getAllergies() != null) {
            patient.setAllergies(request.getAllergies());
        }
        if (request.getRelativeFullName() != null) {
            patient.setRelativeFullName(request.getRelativeFullName());
        }
        if (request.getRelativePhoneNumber() != null) {
            patient.setRelativePhoneNumber(request.getRelativePhoneNumber());
        }
        if (request.getRelativeRelationship() != null) {
            patient.setRelativeRelationship(request.getRelativeRelationship());
        }
        
        Patient saved = patientRepository.save(patient);
        PatientResponse response = patientMapper.entityToResponse(saved);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
    }
    
    /**
     * Get patient statistics for reporting.
     * Pre-aggregated data for report-service consumption.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PatientStatsResponse>> getStats() {
        // Count totals
        long totalPatients = patientRepository.count();
        
        // New patients this month and year
        LocalDate today = LocalDate.now();
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant();
        Instant startOfYear = today.withDayOfYear(1).atStartOfDay(zoneId).toInstant();
        
        long newThisMonth = patientRepository.countByCreatedAtAfter(startOfMonth);
        long newThisYear = patientRepository.countByCreatedAtAfter(startOfYear);
        
        // Count by gender
        Map<String, Integer> byGender = new HashMap<>();
        for (Object[] row : patientRepository.countByGender()) {
            String gender = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            byGender.put(gender, count);
        }
        
        // Count by blood type
        Map<String, Integer> byBloodType = new HashMap<>();
        for (Object[] row : patientRepository.countByBloodType()) {
            String bloodType = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            byBloodType.put(bloodType, count);
        }
        
        // Calculate average age
        List<LocalDate> dobs = patientRepository.findAllDateOfBirths();
        double averageAge = 0;
        if (!dobs.isEmpty()) {
            long totalAge = dobs.stream()
                .mapToLong(dob -> Period.between(dob, today).getYears())
                .sum();
            averageAge = Math.round((totalAge * 10.0 / dobs.size())) / 10.0;
        }
        
        // Registration trend - get from last 30 days
        Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay(zoneId).toInstant();
        Instant tomorrow = today.plusDays(1).atStartOfDay(zoneId).toInstant();
        List<PatientStatsResponse.RegistrationTrend> registrationTrend = new ArrayList<>();
        for (Object[] row : patientRepository.countByCreatedAtGroupedByDate(thirtyDaysAgo, tomorrow)) {
            LocalDate date = null;
            if (row[0] != null) {
                if (row[0] instanceof java.sql.Date) {
                    date = ((java.sql.Date) row[0]).toLocalDate();
                } else if (row[0] instanceof LocalDate) {
                    date = (LocalDate) row[0];
                } else {
                    date = LocalDate.parse(row[0].toString().substring(0, 10));
                }
            }
            int count = ((Number) row[1]).intValue();
            if (date != null) {
                registrationTrend.add(PatientStatsResponse.RegistrationTrend.builder()
                        .date(date)
                        .newPatients(count)
                        .build());
            }
        }
        
        PatientStatsResponse stats = PatientStatsResponse.builder()
            .totalPatients((int) totalPatients)
            .newPatientsThisMonth((int) newThisMonth)
            .newPatientsThisYear((int) newThisYear)
            .patientsByGender(byGender)
            .patientsByBloodType(byBloodType)
            .registrationTrend(registrationTrend)
            .averageAge(averageAge)
            .generatedAt(Instant.now())
            .build();
        
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    /**
     * Upload profile image for a patient.
     * Max file size: 2MB. Allowed types: JPEG, PNG, WebP.
     */
    @PostMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PatientResponse>> uploadProfileImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Patient not found"));
        
        // Delete old image if exists
        if (patient.getProfileImageUrl() != null) {
            fileStorageService.deleteFile(patient.getProfileImageUrl());
        }
        
        // Upload new image
        String imageUrl = fileStorageService.uploadProfileImage(file, id);
        patient.setProfileImageUrl(imageUrl);
        Patient saved = patientRepository.save(patient);
        
        return ResponseEntity.ok(ApiResponse.ok("Profile image uploaded successfully", 
                patientMapper.entityToResponse(saved)));
    }

    /**
     * Delete profile image for a patient.
     */
    @DeleteMapping("/{id}/profile-image")
    public ResponseEntity<ApiResponse<PatientResponse>> deleteProfileImage(@PathVariable String id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Patient not found"));
        
        if (patient.getProfileImageUrl() != null) {
            fileStorageService.deleteFile(patient.getProfileImageUrl());
            patient.setProfileImageUrl(null);
            patientRepository.save(patient);
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Profile image deleted successfully", 
                patientMapper.entityToResponse(patient)));
    }
}
