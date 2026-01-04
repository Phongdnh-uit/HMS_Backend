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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;

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
                .cors(cors -> {}) // Enable CORS - picks up CorsWebFilter bean
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints (no auth required)
                        .pathMatchers(SecurityConstant.PUBLIC_URLS).permitAll()
                        
                        // ============================================
                        // ADMIN ONLY - Account & HR Management
                        // ============================================
                        // Account search allowed for RECEPTIONIST (to link patient accounts)
                        .pathMatchers(HttpMethod.GET, "/api/auth/accounts/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.POST, "/api/auth/accounts/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/auth/accounts/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/auth/accounts/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/hr/departments/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/departments/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hr/departments/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/hr/employees/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/employees/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hr/employees/**").hasAuthority("ADMIN")
                        
                        // ============================================
                        // STAFF + PATIENT - Read HR Data (for booking)
                        // ============================================
                        .pathMatchers(HttpMethod.GET, "/api/hr/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST", "PATIENT")
                        
                        // ============================================
                        // MEDICAL STAFF - Exams & Prescriptions
                        // ============================================
                        // Lab Tests catalog - ADMIN manages, all view
                        .pathMatchers(HttpMethod.POST, "/api/exams/lab-tests/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/exams/lab-tests/**").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/exams/lab-tests/**").hasAuthority("ADMIN")
                        
                        // Lab Results - DOCTOR/NURSE can order and update
                        .pathMatchers(HttpMethod.POST, "/api/exams/lab-results/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE")
                        .pathMatchers(HttpMethod.PUT, "/api/exams/lab-results/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE")
                        .pathMatchers(HttpMethod.DELETE, "/api/exams/lab-results/images/**").hasAnyAuthority("ADMIN", "DOCTOR")
                        
                        // Exams - DOCTOR and NURSE can create (NURSE for vital signs), DOCTOR can update
                        .pathMatchers(HttpMethod.POST, "/api/exams/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE")
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
                        // Patient self-service image upload
                        .pathMatchers(HttpMethod.POST, "/api/patients/me/profile-image").hasAuthority("PATIENT")
                        .pathMatchers(HttpMethod.DELETE, "/api/patients/me/profile-image").hasAuthority("PATIENT")
                        // Admin/Receptionist can upload for any patient
                        .pathMatchers(HttpMethod.POST, "/api/patients/*/profile-image").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.DELETE, "/api/patients/*/profile-image").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        // Employee self-service image upload (all staff)
                        .pathMatchers(HttpMethod.GET, "/api/hr/employees/me").hasAnyAuthority("DOCTOR", "NURSE", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/employees/me").hasAnyAuthority("DOCTOR", "NURSE", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.POST, "/api/hr/employees/me/profile-image").hasAnyAuthority("DOCTOR", "NURSE", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.DELETE, "/api/hr/employees/me/profile-image").hasAnyAuthority("DOCTOR", "NURSE", "RECEPTIONIST")
                        // Admin-only employee image upload
                        .pathMatchers(HttpMethod.POST, "/api/hr/employees/*/profile-image").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/hr/employees/*/profile-image").hasAuthority("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/patients/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.POST, "/api/patients/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        
                        // ============================================
                        // APPOINTMENTS - Various roles
                        // ============================================
                        // Walk-in registration - Receptionist/Admin only
                        .pathMatchers(HttpMethod.POST, "/api/appointments/walk-in").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        // Queue viewing - All staff
                        .pathMatchers(HttpMethod.GET, "/api/appointments/queue/**").hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                        // Call next patient - Doctor/Nurse only
                        .pathMatchers(HttpMethod.PATCH, "/api/appointments/queue/call-next/**").hasAnyAuthority("DOCTOR", "NURSE")
                        // Other appointment operations
                        .pathMatchers("/api/appointments/**").authenticated()
                        
                        // ============================================
                        // SCHEDULES - Staff manages, all authenticated can read
                        // ============================================
                        .pathMatchers(HttpMethod.POST, "/api/hr/schedules/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.PUT, "/api/hr/schedules/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.GET, "/api/hr/schedules/**").authenticated()
                        
                        // ============================================
                        // BILLING - Admin and Receptionist manage, Patient can view/pay their own
                        // ============================================
                        // Patient self-service: /invoices/my (uses JWT token)
                        .pathMatchers(HttpMethod.GET, "/api/invoices/my").hasAuthority("PATIENT")
                        // Invoice mutations - staff only
                        .pathMatchers(HttpMethod.POST, "/api/invoices/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.PUT, "/api/invoices/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")
                        .pathMatchers(HttpMethod.DELETE, "/api/invoices/**").hasAuthority("ADMIN")
                        // Invoice read - staff and patient
                        .pathMatchers(HttpMethod.GET, "/api/invoices/**").hasAnyAuthority("ADMIN", "RECEPTIONIST", "PATIENT")
                        
                        // Payments - patient can pay, staff can manage
                        .pathMatchers(HttpMethod.POST, "/api/payments/init").authenticated()  // Anyone can initiate payment
                        .pathMatchers(HttpMethod.GET, "/api/payments/vnpay-return").authenticated()  // VNPay callback
                        .pathMatchers(HttpMethod.POST, "/api/payments/**").hasAnyAuthority("ADMIN", "RECEPTIONIST")  // Cash payments
                        .pathMatchers(HttpMethod.GET, "/api/payments/**").hasAnyAuthority("ADMIN", "RECEPTIONIST", "PATIENT")
                        .pathMatchers("/api/vnpay/**").authenticated()  // VNPay payment
                        
                        // ============================================
                        // REPORTS - Admin only for analytics
                        // ============================================
                        .pathMatchers("/api/reports/**").hasAuthority("ADMIN")
                        
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
     * Handles both single string role (e.g., "PATIENT") and array roles.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return jwt -> {
            // Extract role from JWT claim
            Object roleClaim = jwt.getClaim("role");
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            
            if (roleClaim instanceof String) {
                // Single role string (e.g., "PATIENT")
                authorities.add(new SimpleGrantedAuthority((String) roleClaim));
            } else if (roleClaim instanceof Collection) {
                // Array of roles
                ((Collection<?>) roleClaim).forEach(role -> 
                    authorities.add(new SimpleGrantedAuthority(role.toString()))
                );
            }
            
            return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
        };
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
