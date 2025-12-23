package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Appointment report response DTO.
 * Contains aggregated appointment data for a given period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReportResponse implements Serializable {
    
    private Period period;
    private int totalAppointments;
    private Map<String, Integer> appointmentsByStatus;
    private Map<String, Integer> appointmentsByType;
    private List<DepartmentStats> appointmentsByDepartment;
    private List<DailyCount> dailyTrend;  // Daily appointment counts
    private double averagePerDay;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount implements Serializable {
        private LocalDate date;
        private int count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period implements Serializable {
        private LocalDate startDate;
        private LocalDate endDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentStats implements Serializable {
        private String departmentId;
        private String departmentName;
        private int count;
        private double percentage;
    }
}
