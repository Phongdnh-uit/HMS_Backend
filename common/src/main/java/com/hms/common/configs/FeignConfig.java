package com.hms.common.configs;

import com.hms.common.exceptions.FeignCustomErrorDecoder;
import com.hms.common.securities.UserContext;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignCustomErrorDecoder();
    }

    /**
     * Request interceptor to forward user context headers to downstream services.
     * This ensures that when service A calls service B, the user identity is preserved.
     */
    @Bean
    public RequestInterceptor userContextRequestInterceptor() {
        return requestTemplate -> {
            UserContext.User user = UserContext.getUser();
            if (user != null) {
                if (user.getId() != null) {
                    requestTemplate.header("X-User-ID", user.getId());
                }
                if (user.getRole() != null) {
                    requestTemplate.header("X-User-Role", user.getRole());
                }
                if (user.getEmail() != null) {
                    requestTemplate.header("X-User-Email", user.getEmail());
                }
            }
        };
    }

    /**
     * Timeout configuration for Feign clients.
     * - Connect timeout: 5 seconds (time to establish connection)
     * - Read timeout: 10 seconds (time to wait for response)
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,   // connect timeout
                10, TimeUnit.SECONDS,  // read timeout
                true                   // follow redirects
        );
    }

    /**
     * Retry configuration for Feign clients.
     * Retries on IO exceptions (connection failures), NOT on HTTP errors.
     * - Max 3 attempts (initial + 2 retries)
     * - Starting interval: 100ms
     * - Max interval: 1 second
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}
