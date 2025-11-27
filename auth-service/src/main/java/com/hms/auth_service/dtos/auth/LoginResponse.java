package com.hms.auth_service.dtos.auth;

import com.hms.auth_service.dtos.account.AccountResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private AccountResponse account;
}
