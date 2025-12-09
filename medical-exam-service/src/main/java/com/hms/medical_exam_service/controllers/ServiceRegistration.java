package com.hms.medical_exam_service.controllers;

import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.hooks.MedicalExamHook;
import com.hms.medical_exam_service.mappers.MedicalExamMapper;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Service registration for GenericService beans.
 * 
 * Note: PrescriptionController doesn't use GenericService because
 * prescriptions are immutable (no update/delete via standard CRUD).
 * It directly uses repository + hook.
 */
@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {

    private final ApplicationContext context;

    /**
     * MedicalExam CRUD service.
     * Used by MedicalExamController for standard CRUD operations.
     */
    @Bean
    CrudService<MedicalExam, String, MedicalExamRequest, MedicalExamResponse> medicalExamService() {
        return new GenericService<>(
            context.getBean(MedicalExamRepository.class),
            context.getBean(MedicalExamMapper.class),
            context.getBean(MedicalExamHook.class)
        );
    }
    
    // Note: No PrescriptionService bean here.
    // PrescriptionController handles operations directly with repository + hook
    // because prescriptions are immutable (no standard update/delete).
}
