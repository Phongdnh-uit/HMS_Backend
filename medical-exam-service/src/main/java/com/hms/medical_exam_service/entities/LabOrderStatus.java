package com.hms.medical_exam_service.entities;

/**
 * Status of a lab order
 */
public enum LabOrderStatus {
    ORDERED,      // Đã chỉ định - tests ordered but not started
    IN_PROGRESS,  // Đang thực hiện - some tests completed
    COMPLETED,    // Hoàn thành - all tests completed
    CANCELLED     // Đã hủy
}
