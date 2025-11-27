package com.hms.auth_service.securities;

import com.hms.auth_service.configs.JwtConfig;
import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;

@RequiredArgsConstructor
@Component
public class TokenProvider {
    private final AccountRepository accountRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${jwt.access-token.expiration}")
    private Long expirationTime;

    public String generateAccessToken(Account account) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("self")
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(expirationTime))
                        .subject(account.getId())
                        .claim("role", account.getRole().name())
                        .claim("email", account.getEmail())
                        .build();
        JwsHeader jwsHeader = JwsHeader.with(JwtConfig.JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public Jwt validateJwt(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            assert jwt.getExpiresAt() != null;
            if (jwt.getExpiresAt().isBefore(Instant.now())) {
                throw new ApiException(ErrorCode.TOKEN_EXPIRED);
            }
            return jwt;
        } catch (JwtException e) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
    }
}
