package com.hms.medical_exam_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.services.CrudService;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.mappers.MedicalExamMapper;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Medical Exam operations.
 * 
 * Endpoints:
 * - GET /exams/all - List all exams (with filter, pagination)
 * - GET /exams/{id} - Get exam by ID
 * - GET /exams/by-appointment/{appointmentId} - Get exam by appointment
 * - POST /exams - Create exam
 * - PUT /exams/{id} - Update exam
 * - DELETE /exams/{id} - Delete exam (blocked in hook)
 */
@RestController
@RequestMapping("/exams")
public class MedicalExamController extends GenericController<MedicalExam, String, MedicalExamRequest, MedicalExamResponse> {

    private final MedicalExamRepository medicalExamRepository;
    private final MedicalExamMapper medicalExamMapper;

    public MedicalExamController(
            CrudService<MedicalExam, String, MedicalExamRequest, MedicalExamResponse> service,
            MedicalExamRepository medicalExamRepository,
            MedicalExamMapper medicalExamMapper) {
        super(service);
        this.medicalExamRepository = medicalExamRepository;
        this.medicalExamMapper = medicalExamMapper;
    }

    /**
     * Get medical exam by appointment ID.
     * Since there's a 1:1 relationship (one exam per appointment), this returns a single exam.
     * 
     * @param appointmentId The appointment ID
     * @return The medical exam for this appointment
     */
    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<MedicalExamResponse>> getByAppointment(
            @PathVariable String appointmentId) {
        
        MedicalExam exam = medicalExamRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ApiException(ErrorCode.EXAM_NOT_FOUND, 
                "No medical exam found for appointment: " + appointmentId));
        
        MedicalExamResponse response = medicalExamMapper.entityToResponse(exam);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
