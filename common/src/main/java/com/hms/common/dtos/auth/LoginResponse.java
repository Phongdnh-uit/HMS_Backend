package com.hms.common.dtos.auth;

import com.hms.common.dtos.account.AccountResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private AccountResponse account;
}
