package com.hms.medical_exam_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used for cross-service communication.
 * 
 * Uses @LoadBalanced to enable service discovery via Eureka.
 * Services can be called by name (e.g., http://medicine-service/api/medicines)
 * instead of hardcoded URLs.
 */
@Configuration
public class WebClientConfig {
    
    /**
     * Creates a load-balanced WebClient builder.
     * 
     * Usage in hooks/services:
     * <pre>
     * webClientBuilder.baseUrl("http://medicine-service").build()
     *     .get()
     *     .uri("/api/medicines/{id}", id)
     *     .retrieve()
     *     .bodyToMono(MedicineResponse.class)
     *     .block();
     * </pre>
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
