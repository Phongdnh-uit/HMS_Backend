package com.hms.medical_exam_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan(basePackages = {"com.hms.medical_exam_service", "com.hms.common"})
public class MedicalExamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalExamServiceApplication.class, args);
    }
}
