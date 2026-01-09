package com.hms.billing_service.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8087}")
    private String serverPort;

    @Bean
    public OpenAPI billingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Billing Service API")
                        .version("1.0.0")
                        .description("REST API for invoice generation, payment processing (VNPay integration), and billing management in HMS")
                        .contact(new Contact()
                                .name("HMS Development Team")
                                .email("dev@hospital.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Direct Service Access (Development)"),
                        new Server()
                                .url("http://localhost:8080/api/invoices")
                                .description("Via API Gateway - Invoices (Production)"),
                        new Server()
                                .url("http://localhost:8080/api/payments")
                                .description("Via API Gateway - Payments (Production)")
                ));
    }
}
