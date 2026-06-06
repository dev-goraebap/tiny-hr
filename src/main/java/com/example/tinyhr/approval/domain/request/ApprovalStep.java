package com.example.tinyhr.approval.domain.request;

import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결재 ledger 의 단일 도장(불변 값). 결재 요청의 결재 이력으로 누적된다.
 *
 * <p>승인이면 {@code note}=코멘트(선택), 반려면 {@code note}=사유(필수).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalStep {

    @Column(name = "step_id", length = 36)
    private String stepId;

    @Column(name = "approver_id", length = 36)
    private String approverId;

    /** 결재 순서(1·2·3차). */
    @Column(name = "step_order")
    private int order;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_kind", length = 16)
    private ApprovalDecisionKind decisionKind;

    /** 승인 코멘트 또는 반려 사유. */
    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "decided_at")
    private Instant decidedAt;

    static ApprovalStep of(
            String stepId,
            String approverId,
            int order,
            ApprovalDecisionKind decisionKind,
            String note,
            Instant decidedAt) {
        ApprovalStep s = new ApprovalStep();
        s.stepId = stepId;
        s.approverId = approverId;
        s.order = order;
        s.decisionKind = decisionKind;
        s.note = note;
        s.decidedAt = decidedAt;
        return s;
    }
}
