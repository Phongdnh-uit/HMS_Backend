package com.hms.common.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(
        name =
                "auth-service",
        configuration = FeignConfig.class)
public interface AccountClient {
    @GetMapping("/accounts/{id}")
    ApiResponse<AccountResponse> findById(@PathVariable("id") String id);

    @PostMapping("/accounts")
    ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody AccountRequest accountRequest);
}
