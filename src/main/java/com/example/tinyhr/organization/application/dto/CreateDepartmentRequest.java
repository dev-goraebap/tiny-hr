package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** parentId 가 null 이면 최상위 부서. */
public record CreateDepartmentRequest(
        @NotBlank @Size(max = 50) String name,
        String parentId) {}
