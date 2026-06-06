package com.example.tinyhr.organization.adapter.mapper.viewmodel;

import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import java.time.LocalDate;

/** 관리자 사원 목록 조회 뷰(읽기 전용). 부서·직급 이름은 조인으로 채운다. */
public record EmployeeListItem(
        String employeeId,
        String name,
        String workEmail,
        String departmentId,
        String departmentName,
        String rankId,
        String rankName,
        Integer rankOrder,
        LocalDate hireDate,
        EmployeeStatus status) {}
