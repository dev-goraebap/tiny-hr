package com.example.tinyhr.iam.application.dto;

import com.example.tinyhr.iam.domain.rbac.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 역할 생성 입력. permissions 의 enum 값 자체는 역직렬화 단계에서 검증된다(중복은 도메인이 정리). */
public record CreateRoleRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 500) String description,
        @NotNull List<Permission> permissions) {}
