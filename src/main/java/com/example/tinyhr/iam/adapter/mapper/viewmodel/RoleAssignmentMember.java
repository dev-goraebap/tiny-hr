package com.example.tinyhr.iam.adapter.mapper.viewmodel;

import java.time.Instant;

/**
 * 특정 역할을 보유한 활성 사원 목록 조회 뷰.
 *
 * <p>표시용으로 organization 의 employee 테이블을 조인한다(읽기 측 프로젝션 — 쓰기 경로는 BC 경계를
 * 넘지 않는다).
 */
public record RoleAssignmentMember(
        String assignmentId,
        String userAccountId,
        String employeeName,
        String employeeWorkEmail,
        Instant assignedAt) {}
