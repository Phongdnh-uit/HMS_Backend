package com.hms.auth_service.controllers;

import com.hms.auth_service.dtos.authentication.ChangePasswordRequest;
import com.hms.auth_service.dtos.authentication.ResetPasswordRequest;
import com.hms.auth_service.securities.TokenProvider;
import com.hms.auth_service.services.AuthService;
import com.hms.common.dtos.Action;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.dtos.auth.LoginRequest;
import com.hms.common.dtos.auth.LoginResponse;
import com.hms.common.dtos.auth.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
@Validated  // Enable method-level validation with groups
public class AuthController {
    private final AuthService authService;
    private final TokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccountResponse>> register(
            @Validated(Action.Create.class) @RequestBody AccountRequest accountRequest
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountResponse>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        // Extract token from "Bearer <token>"
        String token = authHeader.replace("Bearer ", "");
        Jwt jwt = tokenProvider.validateJwt(token);
        String userId = jwt.getSubject();
        AccountResponse account = authService.findById(userId);
        return ResponseEntity.ok(ApiResponse.ok(account));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/send-password-reset-token")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetToken(
            @RequestParam("email") String email) {
        authService.sendPasswordResetToken(email);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/send-verification-email")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(
            @RequestParam("email") String email) {
        authService.sendAccountActivationEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token) {
        authService.activateAccount(token);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
