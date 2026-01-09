package com.hms.auth_service.securities;

import com.hms.auth_service.configs.JwtConfig;
import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.enums.RoleEnum;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.security.oauth2.core.OAuth2Error;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for TokenProvider.
 * Tests JWT token generation and validation logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-AUTH-007/008/009: TokenProvider Unit Tests")
class TokenProviderTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private TokenProvider tokenProvider;

    private static final Long EXPIRATION_TIME = 3600L; // 1 hour

    private Account testAccount;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenProvider, "expirationTime", EXPIRATION_TIME);

        // Setup test account
        testAccount = new Account();
        testAccount.setId(UUID.randomUUID().toString());
        testAccount.setEmail(TestDataFactory.uniqueEmail());
        testAccount.setRole(RoleEnum.PATIENT);
        testAccount.setPassword("encodedPassword");
        testAccount.setEmailVerified(true);
    }

    @Nested
    @DisplayName("Method: generateAccessToken()")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("UC-AUTH-007: Should generate valid JWT token with correct claims")
        void generateAccessToken_withValidAccount_shouldReturnToken() {
            // Given
            Jwt mockJwt = mock(Jwt.class);
            given(mockJwt.getTokenValue()).willReturn("generated.jwt.token");

            given(jwtEncoder.encode(any(JwtEncoderParameters.class))).willReturn(mockJwt);

            // When
            String token = tokenProvider.generateAccessToken(testAccount);

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isEqualTo("generated.jwt.token");

            // Verify the encoder was called with correct parameters
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        }

        @Test
        @DisplayName("Should set correct expiration time in token")
        void generateAccessToken_shouldSetCorrectExpiration() {
            // Given
            Jwt mockJwt = mock(Jwt.class);
            given(mockJwt.getTokenValue()).willReturn("generated.jwt.token");

            given(jwtEncoder.encode(any(JwtEncoderParameters.class))).willReturn(mockJwt);

            // When
            tokenProvider.generateAccessToken(testAccount);

            // Then - use ArgumentCaptor for cleaner verification
            var captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
            then(jwtEncoder).should().encode(captor.capture());
            
            JwtClaimsSet claims = captor.getValue().getClaims();
            Instant issuedAt = claims.getIssuedAt();
            Instant expiresAt = claims.getExpiresAt();

            // Verify expiration is exactly EXPIRATION_TIME seconds after issuance
            assertThat(issuedAt).isNotNull();
            assertThat(expiresAt).isNotNull();
            assertThat(expiresAt.getEpochSecond() - issuedAt.getEpochSecond())
                    .isEqualTo(EXPIRATION_TIME);
        }

        @Test
        @DisplayName("Should include all required claims in token")
        void generateAccessToken_shouldIncludeAllClaims() {
            // Given
            Jwt mockJwt = mock(Jwt.class);
            given(mockJwt.getTokenValue()).willReturn("generated.jwt.token");

            given(jwtEncoder.encode(any(JwtEncoderParameters.class))).willReturn(mockJwt);

            // When
            tokenProvider.generateAccessToken(testAccount);

            // Verify the encoder was called
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        }
    }

    @Nested
    @DisplayName("Method: validateJwt()")
    class ValidateJwtTests {

        @Test
        @DisplayName("UC-AUTH-008: Should validate and return JWT for valid token")
        void validateJwt_withValidToken_shouldReturnJwt() {
            // Given
            String validToken = "valid.jwt.token";
            Jwt mockJwt = mock(Jwt.class);
            given(mockJwt.getExpiresAt()).willReturn(Instant.now().plusSeconds(3600));

            given(jwtDecoder.decode(validToken)).willReturn(mockJwt);

            // When
            Jwt result = tokenProvider.validateJwt(validToken);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockJwt);

            then(jwtDecoder).should().decode(validToken);
        }

        @Test
        @DisplayName("UC-AUTH-009: Should throw exception for expired token")
        void validateJwt_withExpiredToken_shouldThrowException() {
            // Given
            String expiredToken = "expired.jwt.token";
            Jwt mockJwt = mock(Jwt.class);
            given(mockJwt.getExpiresAt()).willReturn(Instant.now().minusSeconds(3600)); // Expired 1 hour ago

            given(jwtDecoder.decode(expiredToken)).willReturn(mockJwt);

            // When/Then
            assertThatThrownBy(() -> tokenProvider.validateJwt(expiredToken))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_EXPIRED);

            then(jwtDecoder).should().decode(expiredToken);
        }

        @Test
        @DisplayName("Should throw exception for invalid token format")
        void validateJwt_withInvalidToken_shouldThrowException() {
            // Given
            String invalidToken = "invalid.token";

            given(jwtDecoder.decode(invalidToken))
                    .willThrow(new JwtException("Invalid JWT token"));

            // When/Then
            assertThatThrownBy(() -> tokenProvider.validateJwt(invalidToken))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);

            then(jwtDecoder).should().decode(invalidToken);
        }

        @Test
        @DisplayName("Should throw exception for malformed token")
        void validateJwt_withMalformedToken_shouldThrowException() {
            // Given
            String malformedToken = "malformed";

            given(jwtDecoder.decode(malformedToken))
                    .willThrow(new BadJwtException("Malformed JWT"));

            // When/Then
            assertThatThrownBy(() -> tokenProvider.validateJwt(malformedToken))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);

            then(jwtDecoder).should().decode(malformedToken);
        }

        @Test
        @DisplayName("Should throw exception for token with invalid signature")
        void validateJwt_withInvalidSignature_shouldThrowException() {
            // Given
            String tokenWithBadSignature = "token.with.badsignature";

            given(jwtDecoder.decode(tokenWithBadSignature))
                    .willThrow(new JwtValidationException("Invalid signature", 
                            Collections.singletonList(new OAuth2Error("invalid_token", "Invalid signature", null))));

            // When/Then
            assertThatThrownBy(() -> tokenProvider.validateJwt(tokenWithBadSignature))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);
        }
    }
}
