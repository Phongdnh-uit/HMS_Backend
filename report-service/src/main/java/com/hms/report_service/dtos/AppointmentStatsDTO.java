package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for receiving appointment stats from appointment-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentStatsDTO {
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    private int totalAppointments;
    private Map<String, Integer> appointmentsByStatus;
    private Map<String, Integer> appointmentsByType;
    
    private List<DepartmentStats> appointmentsByDepartment;
    private List<DoctorStats> appointmentsByDoctor;
    private List<DailyCount> dailyTrend;
    
    private double averagePerDay;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentStats {
        private String departmentName;
        private int count;
        private double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorStats {
        private String doctorId;
        private String doctorName;
        private String departmentName;
        private int count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private LocalDate date;
        private int count;
    }
}
