package com.hms.auth_service.controllers;


import com.hms.auth_service.entities.Account;
import com.hms.auth_service.hooks.AccountHook;
import com.hms.auth_service.mappers.AccountMapper;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.services.CrudService;
import com.hms.common.services.GenericService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {

    private final ApplicationContext context;

    @Bean
    CrudService<Account, String, AccountRequest, AccountResponse> medicineService() {
        return new GenericService<Account, String, AccountRequest, AccountResponse>(
                context.getBean(AccountRepository.class),
                context.getBean(AccountMapper.class),
                context.getBean(AccountHook.class)
        );
    }
}
