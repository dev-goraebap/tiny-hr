package com.example.tinyhr.organization.adapter.mapper.viewmodel;

import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import java.time.Instant;
import java.time.LocalDate;

/** 사원 단건 상세 조회 뷰(읽기 전용). */
public record EmployeeDetailView(
        String employeeId,
        String name,
        String workEmail,
        String departmentId,
        String departmentName,
        String rankId,
        String rankName,
        LocalDate hireDate,
        LocalDate birthDate,
        EmployeeStatus status,
        Instant invitedAt,
        Instant activatedAt,
        Instant terminatedAt) {}
