package com.hms.appointment_service.controllers;

import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.dtos.appointment.CancelAppointmentResponse;
import com.hms.appointment_service.dtos.appointment.CancelRequest;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.mappers.AppointmentMapper;
import com.hms.appointment_service.services.AppointmentService;
import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.services.CrudService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RequestMapping("/appointments")
@RestController
public class AppointmentController extends GenericController<Appointment, String, AppointmentRequest, AppointmentResponse> {

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;

    public AppointmentController(
            CrudService<Appointment, String, AppointmentRequest, AppointmentResponse> service,
            AppointmentService appointmentService,
            AppointmentMapper appointmentMapper) {
        super(service);
        this.appointmentService = appointmentService;
        this.appointmentMapper = appointmentMapper;
    }

    /**
     * Cancel a scheduled appointment.
     * Access: ADMIN, DOCTOR, NURSE, PATIENT (own)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CancelAppointmentResponse>> cancelAppointment(
            @PathVariable String id,
            @Valid @RequestBody CancelRequest request) {
        Appointment appointment = appointmentService.cancelAppointment(id, request.getCancelReason());
        CancelAppointmentResponse response = appointmentMapper.toCancelResponse(appointment);
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled successfully", response));
    }

    /**
     * Complete a scheduled appointment.
     * Access: DOCTOR only (assigned doctor)
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(
            @PathVariable String id) {
        Appointment appointment = appointmentService.completeAppointment(id);
        AppointmentResponse response = appointmentMapper.entityToResponse(appointment);
        return ResponseEntity.ok(ApiResponse.ok("Appointment completed successfully", response));
    }

    /**
     * Bulk cancel appointments for a doctor on a specific date.
     * Called by hr-service when a schedule is cancelled.
     */
    @PostMapping("/bulk-cancel")
    public ResponseEntity<ApiResponse<Integer>> bulkCancelByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date,
            @RequestParam("reason") String reason) {
        int cancelledCount = appointmentService.cancelByDoctorAndDate(doctorId, date, reason);
        return ResponseEntity.ok(ApiResponse.ok("Cancelled " + cancelledCount + " appointments", cancelledCount));
    }

    /**
     * Count active (SCHEDULED) appointments for a doctor on a specific date.
     * Called by hr-service to validate if schedule can be deleted.
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> countByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date) {
        int count = appointmentService.countByDoctorAndDate(doctorId, date);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    /**
     * COMPENSATION: Restore cancelled appointments for a doctor on a specific date.
     * Called by hr-service when saga rollback is needed after appointments were cancelled
     * but schedule final update failed.
     */
    @PostMapping("/bulk-restore")
    public ResponseEntity<ApiResponse<Integer>> bulkRestoreByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date) {
        int restoredCount = appointmentService.restoreByDoctorAndDate(doctorId, date);
        return ResponseEntity.ok(ApiResponse.ok("Restored " + restoredCount + " appointments", restoredCount));
    }
}
