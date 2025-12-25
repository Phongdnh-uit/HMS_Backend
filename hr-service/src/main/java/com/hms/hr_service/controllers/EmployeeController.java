package com.hms.hr_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.services.CrudService;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.mappers.EmployeeMapper;
import com.hms.hr_service.repositories.EmployeeRepository;
import com.hms.hr_service.services.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/hr/employees")
@RestController
public class EmployeeController extends GenericController<Employee, String, EmployeeRequest, EmployeeResponse> {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final FileStorageService fileStorageService;

    public EmployeeController(
            CrudService<Employee, String, EmployeeRequest, EmployeeResponse> service,
            EmployeeRepository employeeRepository,
            EmployeeMapper employeeMapper,
            FileStorageService fileStorageService) {
        super(service);
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload profile image for an employee.
     * Max file size: 2MB. Allowed types: JPEG, PNG, WebP.
     */
    @PostMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EmployeeResponse>> uploadProfileImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Employee not found"));
        
        // Delete old image if exists
        if (employee.getProfileImageUrl() != null) {
            fileStorageService.deleteFile(employee.getProfileImageUrl());
        }
        
        // Upload new image
        String imageUrl = fileStorageService.uploadProfileImage(file, id);
        employee.setProfileImageUrl(imageUrl);
        Employee saved = employeeRepository.save(employee);
        
        return ResponseEntity.ok(ApiResponse.ok("Profile image uploaded successfully", 
                employeeMapper.entityToResponse(saved)));
    }

    /**
     * Delete profile image for an employee.
     */
    @DeleteMapping("/{id}/profile-image")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deleteProfileImage(@PathVariable String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Employee not found"));
        
        if (employee.getProfileImageUrl() != null) {
            fileStorageService.deleteFile(employee.getProfileImageUrl());
            employee.setProfileImageUrl(null);
            employeeRepository.save(employee);
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Profile image deleted successfully", 
                employeeMapper.entityToResponse(employee)));
    }
}
