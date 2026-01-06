package com.hms.medical_exam_service.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.medical_exam_service.dtos.lab.*;
import com.hms.medical_exam_service.services.LabOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Lab Order management.
 * 
 * Endpoints:
 * - GET /exams/lab-orders/all - List all orders (with pagination)
 * - GET /exams/lab-orders/{id} - Get order by ID with results
 * - GET /exams/lab-orders/exam/{examId} - Get orders for a medical exam
 * - GET /exams/lab-orders/patient/{patientId} - Get orders for a patient
 * - POST /exams/lab-orders - Create new order with multiple tests
 * - PUT /exams/lab-orders/{id} - Update order status/priority
 * - DELETE /exams/lab-orders/{id} - Cancel order
 * - POST /exams/lab-orders/auto-group - Auto-group existing ungrouped results
 */
@RestController
@RequestMapping("/exams/lab-orders")
@RequiredArgsConstructor
@Slf4j
public class LabOrderController {

    private final LabOrderService service;

    /**
     * List all lab orders with pagination
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<LabOrderResponse>>> findAll(Pageable pageable) {
        Page<LabOrderResponse> page = service.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.fromPage(page)));
    }

    /**
     * Get lab order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabOrderResponse>> findById(@PathVariable String id) {
        LabOrderResponse response = service.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get all orders for a medical exam
     */
    @GetMapping("/exam/{examId}")
    public ResponseEntity<ApiResponse<List<LabOrderResponse>>> findByExam(@PathVariable String examId) {
        List<LabOrderResponse> results = service.findByMedicalExam(examId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    /**
     * Get all orders for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<LabOrderResponse>>> findByPatient(@PathVariable String patientId) {
        List<LabOrderResponse> results = service.findByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    /**
     * Create a new lab order with multiple tests
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LabOrderResponse>> create(
            @Valid @RequestBody LabOrderRequest request) {
        LabOrderResponse result = service.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Lab order created successfully", result));
    }

    /**
     * Update lab order status/priority
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LabOrderResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody LabOrderUpdateRequest request) {
        LabOrderResponse result = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Lab order updated successfully", result));
    }

    /**
     * Cancel a lab order
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String id) {
        service.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok("Lab order cancelled successfully", null));
    }

    /**
     * Auto-group existing ungrouped lab test results into orders
     * This is useful for migrating existing data
     */
    @PostMapping("/auto-group")
    public ResponseEntity<ApiResponse<Integer>> autoGroup() {
        int ordersCreated = service.autoGroupExistingResults();
        return ResponseEntity.ok(ApiResponse.ok("Auto-grouped " + ordersCreated + " orders", ordersCreated));
    }
}
