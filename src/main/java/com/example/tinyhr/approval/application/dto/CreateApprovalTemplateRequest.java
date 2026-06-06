package com.example.tinyhr.approval.application.dto;

import com.example.tinyhr.approval.domain.ApprovalLineCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 결재선 템플릿 생성 입력. 결재자 중복·순서 검증은 도메인이 수행. */
public record CreateApprovalTemplateRequest(
        @NotNull String departmentId,
        @NotNull ApprovalLineCategory category,
        @NotEmpty @Size(max = 3) @Valid List<ApprovalTemplateApproverRequest> approvers) {}
