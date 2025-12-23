package com.hms.auth_service.services;

import com.hms.auth_service.mappers.AccountMapper;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.auth_service.securities.SecurityUtil;
import com.hms.auth_service.securities.TokenProvider;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.dtos.auth.LoginRequest;
import com.hms.common.dtos.auth.LoginResponse;
import com.hms.common.enums.RoleEnum;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpirationTime;

    @Override
    public AccountResponse register(AccountRequest accountRequest) {
        var account = accountMapper.requestToEntity(accountRequest);

        // Register all set role with PATIENT role by default
        account.setRole(RoleEnum.PATIENT);

        // Encode password
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        // Set email verified to true because we are not implementing email verification now
        account.setEmailVerified(true);

        var savedAccount = accountRepository.save(account);
        return accountMapper.entityToResponse(savedAccount);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 1. ---- Authenticate ----
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
        Authentication authentication =
                authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 2. ---- Set to security holder  ----
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. ---- Generate JWT ----
        String accountId = SecurityUtil.getCurrentUserId();
        assert accountId != null;
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        String jwt = tokenProvider.generateAccessToken(account);
        String refreshToken = UUID.randomUUID().toString();

        account.setRefreshToken(refreshToken);
        account.setRefreshTokenExpiresAt(Instant.now().plusSeconds(refreshTokenExpirationTime));
        accountRepository.save(account);

        // 4. ---- Response ----
        LoginResponse response = new LoginResponse();
        response.setAccessToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setAccount(accountMapper.entityToResponse(account));
        return response;
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        var account = accountRepository.findOne(
                (root, _, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("refreshToken"), refreshToken)
        ).orElseThrow(
                () -> new ApiException(ErrorCode.TOKEN_INVALID)
        );
        if (account.getRefreshTokenExpiresAt() == null || account.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        String jwt = tokenProvider.generateAccessToken(account);
        String newRefreshToken = UUID.randomUUID().toString();
        account.setRefreshToken(newRefreshToken);
        accountRepository.save(account);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(jwt);
        response.setRefreshToken(newRefreshToken);
        response.setAccount(accountMapper.entityToResponse(account));
        return response;
    }

    @Override
    public void logout(String refreshToken) {
        var account = accountRepository.findOne(
                (root, _, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("refreshToken"), refreshToken)
        ).orElseThrow(
                () -> new ApiException(ErrorCode.TOKEN_INVALID)
        );
        account.setRefreshToken(null);
        accountRepository.save(account);
    }

    @Override
    public AccountResponse findById(String userId) {
        var account = accountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        return accountMapper.entityToResponse(account);
    }
}
