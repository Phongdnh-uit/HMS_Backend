package com.hms.report_service.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.report_service.dtos.AppointmentReportResponse;
import com.hms.report_service.dtos.PatientReportResponse;
import com.hms.report_service.dtos.RevenueReportResponse;
import com.hms.report_service.services.AppointmentReportService;
import com.hms.report_service.services.PatientReportService;
import com.hms.report_service.services.RevenueReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for report endpoints.
 * Provides analytics and reporting APIs with Redis caching.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final RevenueReportService revenueReportService;
    private final AppointmentReportService appointmentReportService;
    private final PatientReportService patientReportService;

    /**
     * GET /reports/revenue - Generate revenue report
     * @param startDate Report start date (YYYY-MM-DD)
     * @param endDate Report end date (YYYY-MM-DD)
     * @param departmentId Optional department filter
     * @return Revenue report with aggregated financial data
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String departmentId) {
        
        log.info("Revenue report requested: {} to {}, department: {}", startDate, endDate, departmentId);
        
        if (startDate.isAfter(endDate)) {
            ApiResponse<RevenueReportResponse> errorResponse = new ApiResponse<>();
            errorResponse.setCode(400);
            errorResponse.setMessage("startDate cannot be after endDate");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        RevenueReportResponse report = revenueReportService.generateRevenueReport(startDate, endDate, departmentId);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    /**
     * GET /reports/appointments - Generate appointment statistics report
     * @param startDate Report start date (YYYY-MM-DD)
     * @param endDate Report end date (YYYY-MM-DD)
     * @return Appointment statistics report
     */
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<AppointmentReportResponse>> getAppointmentReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Appointment report requested: {} to {}", startDate, endDate);
        
        if (startDate.isAfter(endDate)) {
            ApiResponse<AppointmentReportResponse> errorResponse = new ApiResponse<>();
            errorResponse.setCode(400);
            errorResponse.setMessage("startDate cannot be after endDate");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        AppointmentReportResponse report = appointmentReportService.generateAppointmentReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    /**
     * GET /reports/patients - Generate patient activity report
     * @return Patient demographics and activity statistics
     */
    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<PatientReportResponse>> getPatientReport() {
        log.info("Patient report requested");
        PatientReportResponse report = patientReportService.generatePatientReport();
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    /**
     * DELETE /reports/cache - Clear report cache
     * @param reportType Optional specific report type to clear (revenue, appointments, patients)
     * @return Success message with cleared types
     */
    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearCache(
            @RequestParam(required = false) String reportType) {
        
        log.info("Cache clear requested for: {}", reportType != null ? reportType : "all");
        
        List<String> clearedTypes = new ArrayList<>();
        
        if (reportType == null || "revenue".equals(reportType)) {
            revenueReportService.clearCache();
            clearedTypes.add("revenue");
        }
        
        if (reportType == null || "appointments".equals(reportType)) {
            appointmentReportService.clearCache();
            clearedTypes.add("appointments");
        }
        
        if (reportType == null || "patients".equals(reportType)) {
            patientReportService.clearCache();
            clearedTypes.add("patients");
        }
        
        if (clearedTypes.isEmpty()) {
            ApiResponse<Map<String, Object>> errorResponse = new ApiResponse<>();
            errorResponse.setCode(400);
            errorResponse.setMessage("Unknown report type: " + reportType + ". Valid types: revenue, appointments, patients");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "message", "Cache cleared successfully",
            "clearedTypes", clearedTypes,
            "clearedAt", Instant.now()
        )));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Report service is healthy"));
    }
}


