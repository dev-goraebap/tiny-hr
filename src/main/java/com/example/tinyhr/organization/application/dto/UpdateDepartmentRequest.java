package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.Size;

/** null 필드는 변경 안 함. */
public record UpdateDepartmentRequest(
        @Size(max = 50) String name,
        @Size(max = 2000) String responsibilities) {}
