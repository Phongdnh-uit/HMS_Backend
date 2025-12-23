package com.hms.appointment_service.controllers;

import com.hms.appointment_service.clients.PatientClient;
import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.dtos.appointment.AppointmentStatsResponse;
import com.hms.appointment_service.dtos.appointment.CancelAppointmentResponse;
import com.hms.appointment_service.dtos.appointment.CancelRequest;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.mappers.AppointmentMapper;
import com.hms.appointment_service.services.AppointmentService;
import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.securities.UserContext;
import com.hms.common.services.CrudService;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RequestMapping("/appointments")
@RestController
@Slf4j
public class AppointmentController extends GenericController<Appointment, String, AppointmentRequest, AppointmentResponse> {

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;
    private final PatientClient patientClient;

    public AppointmentController(
            CrudService<Appointment, String, AppointmentRequest, AppointmentResponse> service,
            AppointmentService appointmentService,
            AppointmentMapper appointmentMapper,
            PatientClient patientClient) {
        super(service);
        this.appointmentService = appointmentService;
        this.appointmentMapper = appointmentMapper;
        this.patientClient = patientClient;
    }

    /**
     * Override findAll to enforce PATIENT role can only see their own appointments.
     * For PATIENT users, this automatically filters by their patientId.
     */
    @Override
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> findAll(
            Pageable pageable,
            @RequestParam(value = "filter", required = false) @Nullable String filter,
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        
        String effectiveFilter = filter;
        
        // Check if current user is PATIENT role
        UserContext.User currentUser = UserContext.getUser();
        if (currentUser != null && "PATIENT".equals(currentUser.getRole())) {
            try {
                // Fetch patient profile to get patientId
                var patientResponse = patientClient.getMyPatientProfile();
                if (patientResponse != null && patientResponse.getData() != null) {
                    String patientId = patientResponse.getData().id();
                    log.info("PATIENT role detected. Enforcing filter for patientId: {}", patientId);
                    
                    // Prepend patient filter to existing filter
                    String patientFilter = "patientId==" + patientId;
                    if (effectiveFilter != null && !effectiveFilter.isBlank()) {
                        effectiveFilter = patientFilter + ";" + effectiveFilter;
                    } else {
                        effectiveFilter = patientFilter;
                    }
                } else {
                    log.warn("PATIENT role but no patient profile found. Returning empty results.");
                    // Return empty page for patients without profile
                    return ResponseEntity.ok(ApiResponse.ok(PageResponse.empty()));
                }
            } catch (Exception e) {
                log.error("Failed to fetch patient profile for PATIENT role: {}", e.getMessage());
                return ResponseEntity.ok(ApiResponse.ok(PageResponse.empty()));
            }
        }
        
        Specification<Appointment> specification = RSQLJPASupport.toSpecification(effectiveFilter);
        if (all) {
            pageable = Pageable.unpaged(pageable.getSort());
        }
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }

    /**
     * Get available time slots for a doctor on a specific date.
     */
    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<java.util.List<com.hms.appointment_service.dtos.appointment.TimeSlotResponse>>> getAvailableSlots(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getAvailableSlots(doctorId, date)));
    }

    /**
     * Get appointments for a specific patient.
     * Used by patient detail page to show only that patient's appointments.
     */
    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<ApiResponse<com.hms.common.dtos.PageResponse<AppointmentResponse>>> getByPatient(
            @PathVariable String patientId,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getByPatientId(patientId, pageable)));
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
    
    /**
     * Get appointment statistics for reporting.
     * Pre-aggregated data for report-service consumption.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AppointmentStatsResponse>> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var stats = appointmentService.getStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}

