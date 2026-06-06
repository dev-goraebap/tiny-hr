package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 사원 초대 요청.
 * departmentId·rankId 는 선택(null = 미배정/미지정).
 */
public record CreateEmployeeRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Email String workEmail,
        String departmentId,
        String rankId,
        @NotNull LocalDate hireDate) {}
