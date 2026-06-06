package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 사원 조직 배치·기본 정보 수정 요청. 각 필드는 null 이면 "변경 안 함".
 * (부서 해제는 별도 clear-department 엔드포인트)
 */
public record UpdateEmployeeRequest(
        @Size(max = 64) String name,
        @Email String workEmail,
        String departmentId,
        String rankId) {}
