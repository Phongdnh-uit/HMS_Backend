package com.hms.common.test;

import org.assertj.core.api.AbstractAssert;
import com.hms.common.dtos.ApiResponse;

/**
 * Custom AssertJ assertion for ApiResponse objects.
 * Provides fluent assertions specifically for HMS API responses.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * ApiResponse<PatientResponse> response = ...;
 * 
 * ApiResponseAssert.assertThat(response)
 *     .isSuccessful()
 *     .hasData()
 *     .dataMatches(patient -> patient.getName().equals("John"));
 * }
 * </pre>
 */
public class ApiResponseAssert<T> extends AbstractAssert<ApiResponseAssert<T>, ApiResponse<T>> {

    private ApiResponseAssert(ApiResponse<T> actual) {
        super(actual, ApiResponseAssert.class);
    }

    /**
     * Entry point for ApiResponse assertions.
     */
    public static <T> ApiResponseAssert<T> assertThat(ApiResponse<T> actual) {
        return new ApiResponseAssert<>(actual);
    }

    /**
     * Verify the response is not null.
     */
    public ApiResponseAssert<T> isNotNull() {
        if (actual == null) {
            failWithMessage("Expected ApiResponse to be not null");
        }
        return this;
    }

    /**
     * Verify the response indicates success (data is not null or status indicates success).
     */
    public ApiResponseAssert<T> isSuccessful() {
        isNotNull();
        if (actual.getData() == null) {
            failWithMessage("Expected successful response with data, but data was null");
        }
        return this;
    }

    /**
     * Verify the response has data.
     */
    public ApiResponseAssert<T> hasData() {
        isNotNull();
        if (actual.getData() == null) {
            failWithMessage("Expected response to have data, but data was null");
        }
        return this;
    }

    /**
     * Verify the response has no data (null data).
     */
    public ApiResponseAssert<T> hasNoData() {
        isNotNull();
        if (actual.getData() != null) {
            failWithMessage("Expected response to have no data, but got: %s", actual.getData());
        }
        return this;
    }

    /**
     * Verify the data matches a condition.
     */
    public ApiResponseAssert<T> dataMatches(java.util.function.Predicate<T> condition) {
        hasData();
        if (!condition.test(actual.getData())) {
            failWithMessage("Expected data to match condition, but it did not. Data: %s", actual.getData());
        }
        return this;
    }

    /**
     * Verify the data equals expected value.
     */
    public ApiResponseAssert<T> dataEquals(T expected) {
        hasData();
        if (!actual.getData().equals(expected)) {
            failWithMessage("Expected data to equal <%s> but was <%s>", expected, actual.getData());
        }
        return this;
    }

    /**
     * Get the data for further assertions.
     */
    public T getData() {
        hasData();
        return actual.getData();
    }
}
