package com.hms.report_service.services;

import com.hms.report_service.clients.AppointmentClient;
import com.hms.report_service.dtos.AppointmentReportResponse;
import com.hms.report_service.dtos.AppointmentStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
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
 * 
 * Caching strategy:
 * - Fresh data cached for 15 minutes (TTL)
 * - Degraded responses (dataStatus != null) are NOT cached
 * - When TTL expires + downstream unavailable -> returns UNAVAILABLE (honest failure)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReportService {

    private final AppointmentClient appointmentClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /**
     * Generate appointment report for the specified period.
     * Calls appointment-service /appointments/stats endpoint for pre-aggregated data.
     * Results are cached in Redis for 10 minutes.
     * Note: Degraded responses (dataStatus != null) are NOT cached.
     */
    @Cacheable(value = "appointment-reports", key = "#startDate + '-' + #endDate", unless = "#result.dataStatus != null")
    public AppointmentReportResponse generateAppointmentReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating appointment report from {} to {} (fetching from appointment-service)", startDate, endDate);
        
        var circuitBreaker = circuitBreakerFactory.create("reportAppointment");
        var statsResponse = circuitBreaker.run(
            () -> appointmentClient.getAppointmentStats(startDate, endDate),
            throwable -> {
                log.warn("[CB-FALLBACK] Appointment service unavailable: {}", throwable.getMessage());
                return null;
            }
        );
        
        if (statsResponse == null || statsResponse.getData() == null) {
            log.warn("No stats data returned from appointment-service");
            return buildUnavailableReport(startDate, endDate, "Appointment service unavailable");
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
    
    /**
     * Build report indicating data is unavailable.
     * This response is NOT cached (via unless condition).
     */
    private AppointmentReportResponse buildUnavailableReport(LocalDate startDate, LocalDate endDate, String reason) {
        log.warn("Building unavailable appointment report: {}", reason);
        return AppointmentReportResponse.builder()
            .period(AppointmentReportResponse.Period.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .totalAppointments(null)  // null indicates unavailable
            .appointmentsByStatus(null)
            .appointmentsByType(null)
            .appointmentsByDepartment(new ArrayList<>())
            .dailyTrend(new ArrayList<>())
            .averagePerDay(null)
            .generatedAt(Instant.now())
            .dataStatus("UNAVAILABLE: " + reason)
            .build();
    }
}
