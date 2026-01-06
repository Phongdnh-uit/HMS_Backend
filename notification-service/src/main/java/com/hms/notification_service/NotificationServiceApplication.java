package com.hms.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service - Email and notification management.
 * Handles scheduled notifications for follow-up reminders.
 */
@SpringBootApplication(
    scanBasePackages = {"com.hms.notification_service", "com.hms.common"},
    exclude = {DataSourceAutoConfiguration.class}
)
@EnableFeignClients
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
