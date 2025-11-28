package com.hms.patient_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.services.CrudService;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.entities.Patient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/patients")
@RestController
public class PatientController extends GenericController<Patient, String, PatientRequest, PatientResponse> {
    public PatientController(CrudService<Patient, String, PatientRequest, PatientResponse> service) {
        super(service);
    }
}
