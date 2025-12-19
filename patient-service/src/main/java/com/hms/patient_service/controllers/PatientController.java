package com.hms.patient_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.securities.UserContext;
import com.hms.common.services.CrudService;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.dtos.patient.PatientSelfUpdateRequest;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.mappers.PatientMapper;
import com.hms.patient_service.repositories.PatientRepository;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/patients")
@RestController
public class PatientController extends GenericController<Patient, String, PatientRequest, PatientResponse> {
    
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientController(
            CrudService<Patient, String, PatientRequest, PatientResponse> service,
            PatientRepository patientRepository,
            PatientMapper patientMapper) {
        super(service);
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
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
}
