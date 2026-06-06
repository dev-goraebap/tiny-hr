package com.example.tinyhr.approval.application.dto;

import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 결재 진행 입력. 반려(REJECT)면 reason 필수(도메인이 재검증). 승인(APPROVE) 코멘트는 선택.
 */
public record DecideApprovalRequest(
        @NotNull ApprovalDecisionKind action,
        @Size(max = 2000) String comment,
        @Size(max = 2000) String reason) {}
