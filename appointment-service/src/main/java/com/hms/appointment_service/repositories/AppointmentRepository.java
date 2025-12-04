package com.hms.appointment_service.repositories;

import com.hms.appointment_service.entities.Appointment;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends SimpleRepository<Appointment, String> {
}
