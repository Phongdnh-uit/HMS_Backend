package com.hms.auth_service.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hms.auth_service.dtos.authentication.ChangePasswordRequest;
import com.hms.auth_service.dtos.authentication.ResetPasswordRequest;
import com.hms.auth_service.entities.Account;
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
import com.hms.common.securities.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MailService mailService;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordBaseUrl = "http://localhost:3000/auth/reset-password";


    @Value("${app.frontend.activate-account-url}")
    private String activeAccountBaseUrl = "http://localhost:3000/auth/activate-account";

    private final Cache<String, String> passwordResetCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    private final Cache<String, String> accountActivationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpirationTime;

    @Override
    public AccountResponse register(AccountRequest accountRequest) {
        var account = accountMapper.requestToEntity(accountRequest);

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Email is already in use");
        }

        // Register all set role with PATIENT role by default
        account.setRole(RoleEnum.PATIENT);

        // Encode password
        account.setPassword(passwordEncoder.encode(account.getPassword()));

        // Set email verified to true because we are not implementing email verification now
        account.setEmailVerified(false);

        var savedAccount = accountRepository.save(account);
        sendAccountActivationEmail(account.getEmail());
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

    @Override
    public void changePassword(ChangePasswordRequest request) {
//        String userId = SecurityUtil.getCurrentUserId();
        String userId = UserContext.getUser().getId();
        assert userId != null;
        Account user =
                accountRepository
                        .findById(userId)
                        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    Map.of("oldPassword", "Old password is incorrect"));
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(user);
    }

    @Override
    public void sendPasswordResetToken(String email) {
        var account = accountRepository.findOne(
                        (root, _, criteriaBuilder) ->
                                criteriaBuilder.equal(root.get("email"), email
                                ))
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        String token = UUID.randomUUID().toString();
        passwordResetCache.put(token, account.getEmail());
        // In real application, send the token to user's email
        System.out.println("Password reset token for " + email + ": " + token);
        String url = String.format(resetPasswordBaseUrl + "?token=%s", token);
        Map<String, Object> emailParam = Map.of(
                "resetPasswordLink", url,
                "expireMinutes", "5"
        );
        mailService.sendEmailFromTemplate(
                account.getEmail(),
                "Password Reset Request",
                "password-reset",
                emailParam
        );
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String cachedEmail = passwordResetCache.getIfPresent(request.getToken());
        if (cachedEmail == null) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Invalid or expired password reset token");
        }
        var account = accountRepository.findOne(
                        (root, _, criteriaBuilder) ->
                                criteriaBuilder.equal(root.get("email"), cachedEmail
                                ))
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        passwordResetCache.invalidate(request.getToken());
    }

    @Override
    public void sendAccountActivationEmail(String email) {
        var account = accountRepository.findOne(
                        (root, _, criteriaBuilder) ->
                                criteriaBuilder.equal(root.get("email"), email
                                ))
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        if (account.isEmailVerified()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Account is already activated");
        }
        String token = UUID.randomUUID().toString();
        // In real application, send the token to user's email
        System.out.println("Account activation token for " + email + ": " + token);
        accountActivationCache.put(token, account.getEmail());
        String url = String.format(activeAccountBaseUrl + "?token=%s", token);
        Map<String, Object> emailParam = Map.of(
                "verificationLink", url,
                "expireMinutes", "1440"
        );
        mailService.sendEmailFromTemplate(
                account.getEmail(),
                "Account Activation",
                "email-verification",
                emailParam
        );
    }

    @Override
    public void activateAccount(String token) {
        String cachedEmail = accountActivationCache.getIfPresent(token);
        if (cachedEmail == null) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "Invalid or expired account activation token");
        }
        var account = accountRepository.findOne(
                        (root, _, criteriaBuilder) ->
                                criteriaBuilder.equal(root.get("email"), cachedEmail
                                ))
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found"));
        account.setEmailVerified(true);
        accountRepository.save(account);
        accountActivationCache.invalidate(token);
    }
}
