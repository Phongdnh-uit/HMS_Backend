package com.hms.auth_service.controllers;

import com.hms.auth_service.services.AuthService;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.dtos.auth.LoginRequest;
import com.hms.common.dtos.auth.LoginResponse;
import com.hms.common.dtos.auth.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccountResponse>> register(
            @Valid @RequestBody AccountRequest accountRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        authService.register(accountRequest)
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        authService.login(loginRequest)
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest refreshRequest
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        authService.refreshToken(refreshRequest.getRefreshToken())
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshRequest refreshRequest
    ) {
        authService.logout(refreshRequest.getRefreshToken());
        return ResponseEntity.ok(
                ApiResponse.ok(
                        null
                )
        );
    }
}
