package com.hms.auth_service.services;

import com.hms.auth_service.dtos.auth.LoginRequest;
import com.hms.auth_service.dtos.auth.LoginResponse;
import com.hms.auth_service.dtos.account.AccountRequest;
import com.hms.auth_service.dtos.account.AccountResponse;

public interface AuthService {
    AccountResponse register(AccountRequest accountRequest);

    LoginResponse login(LoginRequest loginRequest);

    LoginResponse refreshToken(String refreshToken);

    void logout(String refreshToken);
}
