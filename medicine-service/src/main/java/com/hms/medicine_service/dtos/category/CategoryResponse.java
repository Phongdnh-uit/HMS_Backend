package com.hms.medicine_service.dtos.category;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CategoryResponse {
    private String id;

    private String name;

    private String description;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;
}
