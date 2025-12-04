package com.hms.common.helpers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.FeignHandledException;

import java.util.function.Supplier;

public class FeignHelper {
    public static <T> ApiResponse<T> safeCall(Supplier<ApiResponse<T>> call) {
        try {
            return call.get();
        } catch (FeignHandledException ex) {
            return (ApiResponse<T>) ex.getResponse();
        }
    }
}
