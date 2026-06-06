package com.example.tinyhr.iam.adapter.mapper.viewmodel;

import java.time.Instant;

/**
 * 특정 사원의 역할 부여 목록 조회 뷰(회수 포함, 감사용).
 */
public record RoleAssignmentListItem(
        String assignmentId,
        String userAccountId,
        String roleId,
        boolean active,
        Instant assignedAt,
        Instant revokedAt) {}
