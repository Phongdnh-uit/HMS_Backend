package com.hms.auth_service.controllers;

import com.hms.auth_service.entities.Account;
import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.services.CrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/auth/accounts")
@RestController
public class AccountController extends GenericController<Account, String, AccountRequest, AccountResponse> {
    public AccountController(CrudService<Account, String, AccountRequest, AccountResponse> service) {
        super(service);
    }
}
