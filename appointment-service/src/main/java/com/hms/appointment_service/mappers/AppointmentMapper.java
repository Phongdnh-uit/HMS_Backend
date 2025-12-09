package com.hms.appointment_service.mappers;

import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.mappers.GenericMapper;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AppointmentMapper extends GenericMapper<Appointment, AppointmentRequest, AppointmentResponse> {
}
