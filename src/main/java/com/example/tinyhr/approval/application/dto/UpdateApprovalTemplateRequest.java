package com.example.tinyhr.approval.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 결재선 템플릿 결재자 교체 입력(부서·카테고리는 불변). */
public record UpdateApprovalTemplateRequest(
        @NotEmpty @Size(max = 3) @Valid List<ApprovalTemplateApproverRequest> approvers) {}
