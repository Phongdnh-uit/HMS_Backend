package com.hms.common.helpers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.FeignHandledException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class FeignHelper {
    public static <T> ApiResponse<T> safeCall(Supplier<ApiResponse<T>> call) {
        try {
            ApiResponse<T> result = call.get();
            log.debug("✅ [FeignHelper] Feign call successful, data is null: {}", result.getData() == null);
            return result;
        } catch (FeignHandledException ex) {
            log.warn("⚠️ [FeignHelper] Feign call caught exception! Code: {}, Message: {}", 
                ex.getResponse().getCode(), ex.getResponse().getMessage());
            return (ApiResponse<T>) ex.getResponse();
        } catch (Exception ex) {
            log.error("❌ [FeignHelper] Unexpected exception in Feign call: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

