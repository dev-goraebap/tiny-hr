package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** parentId 가 null 이면 최상위 형제들의 재정렬. */
public record ReorderDepartmentsRequest(
        String parentId,
        @NotEmpty List<@NotBlank String> orderedDepartmentIds) {}
