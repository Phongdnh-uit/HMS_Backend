package com.hms.common.configs;

import com.hms.common.exceptions.FeignCustomErrorDecoder;
import feign.Request;
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
