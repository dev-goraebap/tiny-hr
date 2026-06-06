package com.example.tinyhr.organization.application.dto;

import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 사원 기본 정보 일괄 수정 요청. 각 필드는 null 이면 "변경 안 함".
 * status 를 지정하면 활성/퇴직 상태 전이도 함께 처리한다.
 */
public record UpdateEmployeeBasicRequest(
        @Size(max = 64) String name,
        @Email String workEmail,
        String rankId,
        LocalDate hireDate,
        LocalDate birthDate,
        EmployeeStatus status) {}
