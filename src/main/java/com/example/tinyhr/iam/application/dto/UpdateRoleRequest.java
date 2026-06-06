package com.example.tinyhr.iam.application.dto;

import com.example.tinyhr.iam.domain.rbac.Permission;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 역할 부분 수정 입력. null 필드는 "변경 없음"으로 처리한다.
 *
 * <p>MVP 한계: record 로는 "값 없음(absent)"과 "null"을 구분할 수 없어, description 을 null 로
 * 비우는 동작은 지원하지 않는다(null = 변경 없음).
 */
public record UpdateRoleRequest(
        @Size(max = 64) String name,
        @Size(max = 500) String description,
        List<Permission> permissions) {}
