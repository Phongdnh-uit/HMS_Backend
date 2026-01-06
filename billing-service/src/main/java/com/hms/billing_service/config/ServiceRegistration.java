package com.hms.billing_service.config;

import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.dtos.InvoiceResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.hooks.InvoiceHook;
import com.hms.billing_service.mappers.InvoiceMapper;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ServiceRegistration {

    private final ApplicationContext context;

    @Bean
    public CrudService<Invoice, String, InvoiceRequest, InvoiceResponse> invoiceService() {
        return new GenericService<>(
            context.getBean(InvoiceRepository.class),
            context.getBean(InvoiceMapper.class),
            context.getBean(InvoiceHook.class)
        );
    }
}
