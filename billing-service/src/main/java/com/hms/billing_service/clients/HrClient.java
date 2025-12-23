package com.hms.billing_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * Feign client for hr-service.
 * Used to fetch department consultation fee for invoice generation.
 */
@FeignClient(name = "hr-service", path = "/hr")
public interface HrClient {

    @GetMapping("/departments/{id}")
    ApiResponse<DepartmentResponse> getDepartmentById(@PathVariable String id);

    @GetMapping("/employees/{id}")
    ApiResponse<EmployeeResponse> getEmployeeById(@PathVariable String id);

    /**
     * Department response with consultation fee.
     */
    record DepartmentResponse(
        String id,
        String name,
        BigDecimal consultationFee
    ) {}

    /**
     * Employee response with department info.
     */
    record EmployeeResponse(
        String id,
        String fullName,
        String email,
        DepartmentInfo department
    ) {
        public record DepartmentInfo(String id, String name, BigDecimal consultationFee) {}
    }
}
