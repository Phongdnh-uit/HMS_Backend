package com.hms.medicine_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableJpaAuditing
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.hms")
public class MedicineServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicineServiceApplication.class, args);
    }

}
