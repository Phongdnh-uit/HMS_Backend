package com.hms.appointment_service.controllers;

import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.hooks.AppointmentHook;
import com.hms.appointment_service.mappers.AppointmentMapper;
import com.hms.appointment_service.repositories.AppointmentRepository;
import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {

    private final ApplicationContext context;

    @Bean
    CrudService<Appointment, String, AppointmentRequest, AppointmentResponse> patientService() {
        return new GenericService<Appointment, String, AppointmentRequest, AppointmentResponse>(
                context.getBean(AppointmentRepository.class),
                context.getBean(AppointmentMapper.class),
                context.getBean(AppointmentHook.class)
        );
    }


}
