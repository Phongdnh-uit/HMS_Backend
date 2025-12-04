package com.hms.common.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.FeignHandledException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

public class FeignCustomErrorDecoder implements ErrorDecoder {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String body = Util.toString(response.body().asReader());
            ApiResponse<?> apiRes = mapper.readValue(body, ApiResponse.class);

            return new FeignHandledException(apiRes);
        } catch (Exception e) {
            ApiResponse<?> fallback = new ApiResponse<>();
            fallback.setCode(5000);
            fallback.setMessage("Service unavailable");
            fallback.setData(null);

            return new FeignHandledException(fallback);
        }
    }
}
