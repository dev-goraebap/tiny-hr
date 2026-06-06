package com.example.tinyhr.approval.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** 결재선 결재자 입력 항목. */
public record ApprovalTemplateApproverRequest(
        @NotBlank String employeeId,
        @Min(1) @Max(3) int orderNo) {}
