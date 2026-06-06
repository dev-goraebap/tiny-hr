package com.example.tinyhr.approval.domain.request;

import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결재 요청 애그리거트 루트(결재 종류 무관). 상태 머신·결재선 스냅샷·결재 이력을 관리한다.
 *
 * <p>생성 즉시 {@code IN_REVIEW_1}. 각 차수 결재자가 순서대로 승인하면 다음 차수로, 마지막 차수
 * 승인이면 {@code APPROVED}. 반려는 즉시 {@code REJECTED}. 신청자는 진행 중일 때 회수(withdraw)할 수
 * 있고, 취소(cancel)는 종료(REJECTED/CANCELLED) 외 상태에서 가능하다.
 *
 * <p>결재선은 신청 시점에 고정된 1~3명 순서 스냅샷이다(중복 제거).
 */
@Entity
@Table(name = "approval_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalRequest {

    @Id
    @Column(name = "request_id", length = 36)
    private String id;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalRequestKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalRequestStatus status;

    /** 결재선(순서 고정). */
    @ElementCollection
    @CollectionTable(name = "approval_request_approver",
            joinColumns = @JoinColumn(name = "request_id"))
    @OrderColumn(name = "line_order")
    @Column(name = "approver_id", length = 36)
    private List<String> approvalLine = new ArrayList<>();

    /** 결재 이력(누적, append-only). */
    @ElementCollection
    @CollectionTable(name = "approval_step", joinColumns = @JoinColumn(name = "request_id"))
    @OrderColumn(name = "history_index")
    private List<ApprovalStep> approvalHistory = new ArrayList<>();

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** 신규 결재 요청 생성 — 결재선이 1명 이상이면 즉시 IN_REVIEW_1. */
    public static ApprovalRequest create(
            String requestId,
            String requesterId,
            ApprovalRequestKind kind,
            List<String> approvers,
            Instant now) {
        ApprovalRequest r = new ApprovalRequest();
        r.id = requestId;
        r.requesterId = requesterId;
        r.kind = kind;
        r.approvalLine = normalizeLine(approvers);
        r.status = ApprovalRequestStatus.IN_REVIEW_1;
        r.createdAt = now;
        r.submittedAt = now;
        return r;
    }

    /**
     * 현재 차수 결재자가 승인/반려한다.
     *
     * @param stepIdGenerator step 식별자 발급기
     */
    public void decide(
            String approverId,
            ApprovalDecisionKind decisionKind,
            String text,
            Instant now,
            Supplier<String> stepIdGenerator) {
        Integer order = currentOrder();
        if (order == null) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_REQUEST_INVALID_TRANSITION);
        }
        String expected = approvalLine.get(order - 1);
        if (!expected.equals(approverId)) {
            throw new BusinessException(ApprovalErrorCode.APPROVER_MISMATCH);
        }

        String note = normalizeNote(decisionKind, text);
        approvalHistory.add(
                ApprovalStep.of(stepIdGenerator.get(), approverId, order, decisionKind, note, now));

        if (decisionKind == ApprovalDecisionKind.REJECT) {
            status = ApprovalRequestStatus.REJECTED;
            rejectionReason = note;
            decidedAt = now;
            return;
        }

        if (order < approvalLine.size()) {
            status = (order == 1)
                    ? ApprovalRequestStatus.IN_REVIEW_2
                    : ApprovalRequestStatus.IN_REVIEW_3;
            return;
        }
        status = ApprovalRequestStatus.APPROVED;
        decidedAt = now;
    }

    /** 결재 진행 중인 본인 신청을 회수한다. */
    public void withdraw(String requesterId, Instant now) {
        if (!this.requesterId.equals(requesterId)) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_REQUEST_FORBIDDEN);
        }
        if (!status.isInProgress()) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_REQUEST_INVALID_TRANSITION);
        }
        status = ApprovalRequestStatus.CANCELLED;
        cancelledAt = now;
    }

    /** 취소. 이미 REJECTED/CANCELLED 면 거부. reason 은 강제 취소 사유(선택). 승인 후 취소면 wasApproved=true. */
    public boolean cancel(Instant now, String reason) {
        if (status == ApprovalRequestStatus.CANCELLED || status == ApprovalRequestStatus.REJECTED) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_REQUEST_INVALID_TRANSITION);
        }
        boolean wasApproved = status == ApprovalRequestStatus.APPROVED;
        status = ApprovalRequestStatus.CANCELLED;
        cancelledAt = now;
        if (reason != null && !reason.isBlank()) {
            rejectionReason = reason.trim();
        }
        return wasApproved;
    }

    /** 현재 단계 — IN_REVIEW_N 이면 N, 그 외엔 null. */
    public Integer currentOrder() {
        return switch (status) {
            case IN_REVIEW_1 -> 1;
            case IN_REVIEW_2 -> 2;
            case IN_REVIEW_3 -> 3;
            default -> null;
        };
    }

    /** 마지막으로 결재가 일어난 차수. 이력이 없으면 null. */
    public Integer lastDecidedOrder() {
        if (approvalHistory.isEmpty()) {
            return null;
        }
        return approvalHistory.get(approvalHistory.size() - 1).getOrder();
    }

    public List<String> getApprovalLine() {
        return Collections.unmodifiableList(approvalLine);
    }

    public List<ApprovalStep> getApprovalHistory() {
        return Collections.unmodifiableList(approvalHistory);
    }

    private static List<String> normalizeLine(List<String> approvers) {
        if (approvers == null || approvers.isEmpty()) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_LINE_INVALID);
        }
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(approvers));
        if (distinct.size() > 3) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_LINE_INVALID);
        }
        return distinct;
    }

    private static String normalizeNote(ApprovalDecisionKind kind, String text) {
        String trimmed = text == null ? null : text.trim();
        if (kind == ApprovalDecisionKind.REJECT) {
            if (trimmed == null || trimmed.isEmpty()) {
                throw new BusinessException(ApprovalErrorCode.APPROVAL_DECISION_REASON_REQUIRED);
            }
            return trimmed;
        }
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
    }
}
