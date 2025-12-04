package com.hms.common.exceptions.errors;

import com.hms.common.dtos.ApiResponse;
import lombok.Getter;

@Getter
public class FeignHandledException extends RuntimeException {
    private final ApiResponse<?> response;

    public FeignHandledException(ApiResponse<?> response) {
        super(response.getMessage());
        this.response = response;
    }
}
