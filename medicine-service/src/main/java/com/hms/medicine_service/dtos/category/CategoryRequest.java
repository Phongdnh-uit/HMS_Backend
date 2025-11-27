package com.hms.medicine_service.dtos.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
}
