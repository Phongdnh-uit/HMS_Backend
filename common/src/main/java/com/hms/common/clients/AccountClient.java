package com.hms.common.clients;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient("auth-service")
public interface AccountClient {
    @PostMapping("/accounts")
    ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody AccountRequest accountRequest);
}
