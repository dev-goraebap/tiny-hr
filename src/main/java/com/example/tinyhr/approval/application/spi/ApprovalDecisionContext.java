package com.example.tinyhr.approval.application.spi;

import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import java.util.List;

/**
 * 결재 확정 후속 처리에 넘기는 결재 건 스냅샷(읽기 전용 DTO).
 *
 * <p>SPI(타 컨텍스트 도메인)가 approval 애그리거트 타입에 묶이지 않도록 필요한 값만 담아 넘긴다.
 *
 * @param lastDecidedOrder 마지막으로 결재가 일어난 차수(없으면 null)
 */
public record ApprovalDecisionContext(
        String requestId,
        String requesterId,
        ApprovalRequestKind kind,
        ApprovalRequestStatus status,
        List<String> approvers,
        String rejectionReason,
        Integer lastDecidedOrder) {}
