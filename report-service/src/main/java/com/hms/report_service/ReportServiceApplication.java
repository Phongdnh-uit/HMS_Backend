package com.hms.report_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Report Service - Analytics and Reporting
 * Provides cached reports for revenue, appointments, and patients.
 * Uses Redis caching for performance optimization.
 */
@SpringBootApplication(
    scanBasePackages = {"com.hms.report_service", "com.hms.common"},
    exclude = {DataSourceAutoConfiguration.class} // No database needed
)
@EnableFeignClients
@EnableCaching
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
