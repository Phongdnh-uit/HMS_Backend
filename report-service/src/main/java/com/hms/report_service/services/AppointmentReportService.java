package com.hms.report_service.services;

import com.hms.report_service.clients.AppointmentClient;
import com.hms.report_service.dtos.AppointmentReportResponse;
import com.hms.report_service.dtos.AppointmentStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating appointment reports.
 * Uses Redis caching for performance optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReportService {

    private final AppointmentClient appointmentClient;

    /**
     * Generate appointment report for the specified period.
     * Calls appointment-service /appointments/stats endpoint for pre-aggregated data.
     * Results are cached in Redis for 10 minutes.
     */
    @Cacheable(value = "appointment-reports", key = "#startDate + '-' + #endDate")
    public AppointmentReportResponse generateAppointmentReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating appointment report from {} to {} (fetching from appointment-service)", startDate, endDate);
        
        var statsResponse = appointmentClient.getAppointmentStats(startDate, endDate);
        
        if (statsResponse == null || statsResponse.getData() == null) {
            log.warn("No stats data returned from appointment-service");
            return buildEmptyReport(startDate, endDate);
        }
        
        AppointmentStatsDTO stats = statsResponse.getData();
        
        // Map department stats
        List<AppointmentReportResponse.DepartmentStats> departmentStats = new ArrayList<>();
        if (stats.getAppointmentsByDepartment() != null) {
            departmentStats = stats.getAppointmentsByDepartment().stream()
                .map(d -> AppointmentReportResponse.DepartmentStats.builder()
                    .departmentName(d.getDepartmentName())
                    .count(d.getCount())
                    .percentage(d.getPercentage())
                    .build())
                .collect(Collectors.toList());
        }
        // Map daily trend
        List<AppointmentReportResponse.DailyCount> dailyTrend = new ArrayList<>();
        if (stats.getDailyTrend() != null) {
            dailyTrend = stats.getDailyTrend().stream()
                .map(d -> AppointmentReportResponse.DailyCount.builder()
                    .date(d.getDate())
                    .count(d.getCount())
                    .build())
                .collect(Collectors.toList());
        }

        return AppointmentReportResponse.builder()
            .period(AppointmentReportResponse.Period.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .totalAppointments(stats.getTotalAppointments())
            .appointmentsByStatus(stats.getAppointmentsByStatus() != null ? stats.getAppointmentsByStatus() : new HashMap<>())
            .appointmentsByType(stats.getAppointmentsByType() != null ? stats.getAppointmentsByType() : new HashMap<>())
            .appointmentsByDepartment(departmentStats)
            .dailyTrend(dailyTrend)
            .averagePerDay(stats.getAveragePerDay())
            .generatedAt(Instant.now())
            .build();
    }

    /**
     * Clear the appointment report cache.
     */
    @CacheEvict(value = "appointment-reports", allEntries = true)
    public void clearCache() {
        log.info("Appointment report cache cleared");
    }
    
    private AppointmentReportResponse buildEmptyReport(LocalDate startDate, LocalDate endDate) {
        return AppointmentReportResponse.builder()
            .period(AppointmentReportResponse.Period.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .totalAppointments(0)
            .appointmentsByStatus(new HashMap<>())
            .appointmentsByType(new HashMap<>())
            .appointmentsByDepartment(new ArrayList<>())
            .dailyTrend(new ArrayList<>())
            .averagePerDay(0)
            .generatedAt(Instant.now())
            .build();
    }
}
