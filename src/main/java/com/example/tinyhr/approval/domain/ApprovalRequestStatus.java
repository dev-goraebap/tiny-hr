package com.example.tinyhr.approval.domain;

import java.util.Set;

/**
 * 결재 요청 상태(모든 결재 종류 공통 커널).
 *
 * <p>진행: {@code SUBMITTED}·{@code IN_REVIEW_1~3}. 종료: {@code APPROVED}·{@code REJECTED}·
 * {@code CANCELLED}.
 */
public enum ApprovalRequestStatus {
    SUBMITTED,
    IN_REVIEW_1,
    IN_REVIEW_2,
    IN_REVIEW_3,
    APPROVED,
    REJECTED,
    CANCELLED;

    private static final Set<ApprovalRequestStatus> IN_PROGRESS =
            Set.of(SUBMITTED, IN_REVIEW_1, IN_REVIEW_2, IN_REVIEW_3);

    public boolean isInProgress() {
        return IN_PROGRESS.contains(this);
    }

    public boolean isTerminal() {
        return !isInProgress();
    }
}
