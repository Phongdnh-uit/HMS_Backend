package com.hms.common.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.FeignHandledException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignCustomErrorDecoder implements ErrorDecoder {
    private final ObjectMapper mapper;
    
    public FeignCustomErrorDecoder() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule()); // Support LocalTime, LocalDate
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("🔥 [FeignErrorDecoder] Error in Feign call: method={}, status={}", methodKey, response.status());
        
        try {
            String body = Util.toString(response.body().asReader());
            log.warn("🔥 [FeignErrorDecoder] Error response body: {}", body);
            
            ApiResponse<?> apiRes = mapper.readValue(body, ApiResponse.class);

            return new FeignHandledException(apiRes);
        } catch (Exception e) {
            log.error("🔥🔥 [FeignErrorDecoder] Failed to parse error response! Exception: {}", e.getMessage(), e);
            
            ApiResponse<?> fallback = new ApiResponse<>();
            fallback.setCode(5000);
            fallback.setMessage("Service unavailable: " + e.getMessage());
            fallback.setData(null);

            return new FeignHandledException(fallback);
        }
    }
}

