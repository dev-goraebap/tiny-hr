package com.example.tinyhr.iam.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 사원에게 역할을 부여하는 입력. */
public record AssignRoleRequest(
        @NotBlank String userAccountId,
        @NotBlank String roleId) {}
