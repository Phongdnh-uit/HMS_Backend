package com.hms.patient_service.controllers;

import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.hooks.PatientHook;
import com.hms.patient_service.mappers.PatientMapper;
import com.hms.patient_service.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {

    private final ApplicationContext context;

    @Bean
    CrudService<Patient, String, PatientRequest, PatientResponse> patientService() {
        return new GenericService<Patient, String, PatientRequest, PatientResponse>(
                context.getBean(PatientRepository.class),
                context.getBean(PatientMapper.class),
                context.getBean(PatientHook.class)
        );
    }


}
