package com.hms.medical_exam_service.services;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.medical_exam_service.dtos.lab.*;
import com.hms.medical_exam_service.entities.*;
import com.hms.medical_exam_service.mappers.LabOrderMapper;
import com.hms.medical_exam_service.mappers.LabTestResultMapper;
import com.hms.medical_exam_service.repositories.LabOrderRepository;
import com.hms.medical_exam_service.repositories.LabTestRepository;
import com.hms.medical_exam_service.repositories.LabTestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabTestRepository labTestRepository;
    private final LabTestResultRepository labTestResultRepository;
    private final LabOrderMapper labOrderMapper;
    private final LabTestResultMapper labTestResultMapper;

    /**
     * Create a new lab order with multiple tests
     */
    @Transactional
    public LabOrderResponse create(LabOrderRequest request) {
        // Create LabOrder
        LabOrder order = new LabOrder();
        order.setMedicalExamId(request.getMedicalExamId());
        order.setPatientId(request.getPatientId());
        order.setPatientName(request.getPatientName());
        order.setOrderingDoctorId(request.getOrderingDoctorId());
        order.setOrderingDoctorName(request.getOrderingDoctorName());
        order.setOrderDate(Instant.now());
        order.setStatus(LabOrderStatus.ORDERED);
        order.setPriority(request.getPriority() != null ? request.getPriority() : OrderPriority.NORMAL);
        order.setNotes(request.getNotes());
        order.setOrderNumber(generateOrderNumber());

        // Save order first to get ID
        order = labOrderRepository.save(order);

        // Create LabTestResult for each test
        List<LabTestResult> results = new ArrayList<>();
        for (String labTestId : request.getLabTestIds()) {
            LabTest labTest = labTestRepository.findById(labTestId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab test not found: " + labTestId));

            LabTestResult result = new LabTestResult();
            result.setMedicalExamId(request.getMedicalExamId());
            result.setLabOrder(order);
            result.setLabTestId(labTestId);
            result.setLabTestCode(labTest.getCode());
            result.setLabTestName(labTest.getName());
            result.setLabTestCategory(labTest.getCategory());
            result.setLabTestPrice(labTest.getPrice());
            result.setPatientId(request.getPatientId());
            result.setPatientName(request.getPatientName());
            result.setStatus(ResultStatus.PENDING);
            result.setIsAbnormal(false);

            results.add(labTestResultRepository.save(result));
        }

        order.setResults(results);
        log.info("Created lab order {} with {} tests", order.getOrderNumber(), results.size());

        return labOrderMapper.entityToResponse(order);
    }

    /**
     * Get lab order by ID
     */
    public LabOrderResponse findById(String id) {
        LabOrder order = labOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab order not found"));
        return labOrderMapper.entityToResponse(order);
    }

    /**
     * Get all lab orders for a medical exam
     */
    public List<LabOrderResponse> findByMedicalExam(String examId) {
        List<LabOrder> orders = labOrderRepository.findByMedicalExamIdOrderByOrderDateDesc(examId);
        return labOrderMapper.entitiesToResponses(orders);
    }

    /**
     * Get all lab orders for a patient
     */
    public List<LabOrderResponse> findByPatient(String patientId) {
        List<LabOrder> orders = labOrderRepository.findByPatientIdOrderByOrderDateDesc(patientId);
        return labOrderMapper.entitiesToResponses(orders);
    }

    /**
     * Get all lab orders with pagination
     */
    public Page<LabOrderResponse> findAll(Pageable pageable) {
        return labOrderRepository.findAll(pageable).map(labOrderMapper::entityToResponse);
    }

    /**
     * Update lab order status
     */
    @Transactional
    public LabOrderResponse update(String id, LabOrderUpdateRequest request) {
        LabOrder order = labOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab order not found"));

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            order.setPriority(request.getPriority());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        order = labOrderRepository.save(order);
        return labOrderMapper.entityToResponse(order);
    }

    /**
     * Update order status based on test results
     */
    @Transactional
    public void updateOrderStatusFromResults(String orderId) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab order not found"));

        List<LabTestResult> results = order.getResults();
        if (results == null || results.isEmpty()) {
            return;
        }

        long completed = results.stream().filter(r -> r.getStatus() == ResultStatus.COMPLETED).count();
        long total = results.size();

        if (completed == total) {
            order.setStatus(LabOrderStatus.COMPLETED);
        } else if (completed > 0) {
            order.setStatus(LabOrderStatus.IN_PROGRESS);
        }

        labOrderRepository.save(order);
    }

    /**
     * Cancel a lab order
     */
    @Transactional
    public void cancel(String id) {
        LabOrder order = labOrderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lab order not found"));

        if (order.getStatus() == LabOrderStatus.COMPLETED) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Cannot cancel completed order");
        }

        order.setStatus(LabOrderStatus.CANCELLED);
        labOrderRepository.save(order);
    }

    /**
     * Auto-group existing LabTestResults without order into new orders
     * Groups by medicalExamId
     */
    @Transactional
    public int autoGroupExistingResults() {
        List<LabTestResult> ungroupedResults = labTestResultRepository.findByLabOrderIsNull();
        if (ungroupedResults.isEmpty()) {
            return 0;
        }

        // Group by medicalExamId
        var groupedByExam = ungroupedResults.stream()
                .collect(java.util.stream.Collectors.groupingBy(LabTestResult::getMedicalExamId));

        int ordersCreated = 0;
        for (var entry : groupedByExam.entrySet()) {
            String examId = entry.getKey();
            List<LabTestResult> results = entry.getValue();

            // Create new order
            LabOrder order = new LabOrder();
            order.setMedicalExamId(examId);
            order.setOrderDate(results.get(0).getCreatedAt() != null ? results.get(0).getCreatedAt() : Instant.now());
            order.setOrderNumber(generateOrderNumber());
            order.setPriority(OrderPriority.NORMAL);
            order.setNotes("Auto-grouped from existing results");

            // Get patient info from first result
            LabTestResult first = results.get(0);
            order.setPatientId(first.getPatientId());
            order.setPatientName(first.getPatientName());

            // Determine status
            long completed = results.stream().filter(r -> r.getStatus() == ResultStatus.COMPLETED).count();
            if (completed == results.size()) {
                order.setStatus(LabOrderStatus.COMPLETED);
            } else if (completed > 0) {
                order.setStatus(LabOrderStatus.IN_PROGRESS);
            } else {
                order.setStatus(LabOrderStatus.ORDERED);
            }

            order = labOrderRepository.save(order);

            // Link results to order
            for (LabTestResult result : results) {
                result.setLabOrder(order);
                labTestResultRepository.save(result);
            }

            ordersCreated++;
            log.info("Auto-grouped {} results into order {} for exam {}", results.size(), order.getOrderNumber(), examId);
        }

        return ordersCreated;
    }

    /**
     * Generate unique order number: XN-YYYYMMDD-NNN
     */
    private String generateOrderNumber() {
        String prefix = "XN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Integer maxNum = labOrderRepository.findMaxOrderNumberForPrefix(prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return prefix + String.format("%03d", nextNum);
    }
}
