package com.hms.appointment_service.controllers;

import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.controllers.GenericController;
import com.hms.common.services.CrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/appointments")
@RestController
public class AppointmentController extends GenericController<Appointment, String, AppointmentRequest, AppointmentResponse> {
    public AppointmentController(CrudService<Appointment, String, AppointmentRequest, AppointmentResponse> service) {
        super(service);
    }
}
