package com.hms.patient_service.controllers.medicine;

import com.hms.common.controllers.GenericController;
import com.hms.common.services.CrudService;
import com.hms.patient_service.dtos.medicine.MedicineRequest;
import com.hms.patient_service.dtos.medicine.MedicineResponse;
import com.hms.patient_service.entities.Medicine;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/medicines")
@RestController
public class MedicineController extends GenericController<Medicine, String, MedicineRequest, MedicineResponse> {
    public MedicineController(CrudService<Medicine, String, MedicineRequest, MedicineResponse> service) {
        super(service);
    }
}