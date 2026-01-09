package com.hms.api_gateway.configs;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerAggregationConfig {

    @Bean
    public GroupedOpenApi authServiceApi() {
        return GroupedOpenApi.builder()
                .group("auth-service")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi patientServiceApi() {
        return GroupedOpenApi.builder()
                .group("patient-service")
                .pathsToMatch("/api/patients/**")
                .build();
    }

    @Bean
    public GroupedOpenApi medicineServiceApi() {
        return GroupedOpenApi.builder()
                .group("medicine-service")
                .pathsToMatch("/api/medicines/**")
                .build();
    }

    @Bean
    public GroupedOpenApi hrServiceApi() {
        return GroupedOpenApi.builder()
                .group("hr-service")
                .pathsToMatch("/api/hr/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appointmentServiceApi() {
        return GroupedOpenApi.builder()
                .group("appointment-service")
                .pathsToMatch("/api/appointments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi medicalExamServiceApi() {
        return GroupedOpenApi.builder()
                .group("medical-exam-service")
                .pathsToMatch("/api/exams/**", "/api/prescriptions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi billingServiceApi() {
        return GroupedOpenApi.builder()
                .group("billing-service")
                .pathsToMatch("/api/invoices/**", "/api/payments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi reportServiceApi() {
        return GroupedOpenApi.builder()
                .group("report-service")
                .pathsToMatch("/api/reports/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationServiceApi() {
        return GroupedOpenApi.builder()
                .group("notification-service")
                .pathsToMatch("/api/notifications/**")
                .build();
    }
}
