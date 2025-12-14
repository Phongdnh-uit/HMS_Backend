package com.hms.appointment_service.mappers;

import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.dtos.appointment.CancelAppointmentResponse;
import com.hms.appointment_service.dtos.appointment.DoctorResponse;
import com.hms.appointment_service.dtos.appointment.PatientResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.mappers.GenericMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AppointmentMapper extends GenericMapper<Appointment, AppointmentRequest, AppointmentResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "doctorName", ignore = true)
    @Mapping(target = "doctorDepartment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Appointment requestToEntity(AppointmentRequest request);

    @Override
    @Mapping(target = "patient", expression = "java(toPatientResponse(entity))")
    @Mapping(target = "doctor", expression = "java(toDoctorResponse(entity))")
    AppointmentResponse entityToResponse(Appointment entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "doctorId", ignore = true)
    @Mapping(target = "doctorName", ignore = true)
    @Mapping(target = "doctorDepartment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void partialUpdate(AppointmentRequest request, @MappingTarget Appointment entity);

    /**
     * Map appointment to CancelAppointmentResponse.
     */
    default CancelAppointmentResponse toCancelResponse(Appointment entity) {
        if (entity == null) {
            return null;
        }
        return CancelAppointmentResponse.builder()
                .id(entity.getId())
                .patient(toPatientResponse(entity))
                .doctor(toDoctorResponse(entity))
                .status(entity.getStatus())
                .cancelReason(entity.getCancelReason())
                .cancelledAt(entity.getCancelledAt())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    default PatientResponse toPatientResponse(Appointment appointment) {
        if (appointment == null || appointment.getPatientId() == null) {
            return null;
        }
        PatientResponse response = new PatientResponse();
        response.setId(appointment.getPatientId());
        response.setFullName(appointment.getPatientName());
        return response;
    }

    default DoctorResponse toDoctorResponse(Appointment appointment) {
        if (appointment == null || appointment.getDoctorId() == null) {
            return null;
        }
        DoctorResponse response = new DoctorResponse();
        response.setId(appointment.getDoctorId());
        response.setFullName(appointment.getDoctorName());
        response.setDepartment(appointment.getDoctorDepartment());
        return response;
    }
}
