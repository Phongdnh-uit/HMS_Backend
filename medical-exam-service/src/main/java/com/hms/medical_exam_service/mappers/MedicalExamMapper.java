package com.hms.medical_exam_service.mappers;

import com.hms.common.mappers.GenericMapper;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicalExamMapper extends GenericMapper<MedicalExam, MedicalExamRequest, MedicalExamResponse> {

    @Override
    MedicalExam requestToEntity(MedicalExamRequest request);

    @Override
    @Mapping(target = "vitals", expression = "java(mapVitals(entity))")
    @Mapping(target = "appointment.id", source = "appointmentId")
    @Mapping(target = "patient.id", source = "patientId")
    @Mapping(target = "patient.fullName", source = "patientName")
    @Mapping(target = "doctor.id", source = "doctorId")
    @Mapping(target = "doctor.fullName", source = "doctorName")
    MedicalExamResponse entityToResponse(MedicalExam entity);

    @Override
    void partialUpdate(MedicalExamRequest request, @MappingTarget MedicalExam entity);

    /**
     * Maps flat vitals fields from entity to nested VitalsInfo object
     */
    default MedicalExamResponse.VitalsInfo mapVitals(MedicalExam entity) {
        if (entity == null) {
            return null;
        }
        
        MedicalExamResponse.VitalsInfo vitals = new MedicalExamResponse.VitalsInfo();
        vitals.setTemperature(entity.getTemperature());
        vitals.setBloodPressureSystolic(entity.getBloodPressureSystolic());
        vitals.setBloodPressureDiastolic(entity.getBloodPressureDiastolic());
        vitals.setHeartRate(entity.getHeartRate());
        vitals.setWeight(entity.getWeight());
        vitals.setHeight(entity.getHeight());
        
        return vitals;
    }
}
