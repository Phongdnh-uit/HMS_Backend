package com.hms.api_gateway.configs;

import com.hms.api_gateway.constants.SecurityConstant;
import com.nimbusds.jose.util.Base64;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

@Configuration
public class SecurityConfig {
    public static final SignatureAlgorithm JWT_ALGORITHM = SignatureAlgorithm.RS256;

    @Value("${jwt.public-key}")
    private String publicKeyPem;

    private RSAPublicKey publicKey;

    @PostConstruct
    public void initKeys() throws Exception {
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder
                .withPublicKey(this.publicKey)
                .signatureAlgorithm(JWT_ALGORITHM)
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints (no auth required)
                        .pathMatchers(SecurityConstant.PUBLIC_URLS).permitAll()
                        
                        // ============================================
                        // ADMIN ONLY - Account & HR Management
                        // ============================================
                        .pathMatchers("/api/auth/accounts/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/hr/departments/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/departments/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hr/departments/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/hr/employees/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/employees/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hr/employees/**").hasAuthority("ADMIN")
                        
                        // ============================================
                        // STAFF - Read HR Data (Admin, Doctor, Nurse, Receptionist)
                        // ============================================
                        .pathMatchers(HttpMethod.GET, "/api/hr/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                        
                        // ============================================
                        // MEDICAL STAFF - Exams & Prescriptions
                        // ============================================
                        .pathMatchers(HttpMethod.POST, "/api/exams/**").hasAnyAuthority("ADMIN", "DOCTOR")
                        .pathMatchers(HttpMethod.PUT, "/api/exams/**").hasAnyAuthority("ADMIN", "DOCTOR")
                        .pathMatchers(HttpMethod.DELETE, "/api/exams/**").hasAnyAuthority("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/exams/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "PATIENT")
                        
                        // ============================================
                        // MEDICINE - Admin manages, Staff reads
                        // ============================================
                        .pathMatchers(HttpMethod.POST, "/api/medicines/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/medicines/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/medicines/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/medicines/**").hasAnyAuthority("ADMIN", "DOCTOR")
                        .pathMatchers(HttpMethod.GET, "/api/medicines/**").authenticated()
                        
                        // ============================================
                        // PATIENT MANAGEMENT
                        // ============================================
                        .pathMatchers("/api/patients/me/**").authenticated()  // Self-service
                        .pathMatchers(HttpMethod.GET, "/api/patients/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.POST, "/api/patients/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        
                        // ============================================
                        // APPOINTMENTS - Various roles
                        // ============================================
                        .pathMatchers("/api/appointments/**").authenticated()
                        
                        // ============================================
                        // SCHEDULES - Staff manages, all authenticated can read
                        // ============================================
                        .pathMatchers(HttpMethod.POST, "/api/hr/schedules/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/schedules/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.GET, "/api/hr/schedules/**").authenticated()
                        
                        // Default: require authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }
    
    /**
     * Custom JWT authentication converter to extract 'role' claim as authority.
     * This enables hasAuthority("ADMIN"), hasAuthority("DOCTOR"), etc. in security rules.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Don't add "SCOPE_" prefix, use the role value directly
        authoritiesConverter.setAuthorityPrefix("");
        // Extract from 'role' claim instead of default 'scope' or 'scp'
        authoritiesConverter.setAuthoritiesClaimName("role");
        
        ReactiveJwtAuthenticationConverterAdapter converter = new ReactiveJwtAuthenticationConverterAdapter(
            new JwtAuthenticationConverter() {{
                setJwtGrantedAuthoritiesConverter(authoritiesConverter);
            }}
        );
        
        return converter;
    }

    private RSAPublicKey loadPublicKey(String pem) throws Exception {
        String publicKeyString = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.from(publicKeyString).decode();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
