package com.hms.auth_service.services;

import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.dtos.auth.LoginRequest;
import com.hms.common.dtos.auth.LoginResponse;

public interface AuthService {
    AccountResponse register(AccountRequest accountRequest);

    LoginResponse login(LoginRequest loginRequest);

    LoginResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    AccountResponse findById(String userId);
}
