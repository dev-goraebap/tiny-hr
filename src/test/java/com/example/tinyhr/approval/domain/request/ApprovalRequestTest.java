package com.example.tinyhr.approval.domain.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApprovalRequestTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final Supplier<String> stepIds = stepIdGen();

    private static Supplier<String> stepIdGen() {
        AtomicInteger n = new AtomicInteger();
        return () -> "step-" + n.incrementAndGet();
    }

    private static void assertBusiness(ThrowingCallable callable, ApprovalErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static ApprovalRequest create(List<String> approvers) {
        return ApprovalRequest.create("req-1", "requester", ApprovalRequestKind.LEAVE, approvers, NOW);
    }

    @Test
    @DisplayName("생성 즉시 IN_REVIEW_1 이고 결재선이 고정된다")
    void create() {
        ApprovalRequest r = create(List.of("a", "b"));

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.IN_REVIEW_1);
        assertThat(r.getApprovalLine()).containsExactly("a", "b");
        assertThat(r.currentOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("결재선이 비면 생성할 수 없다")
    void create_emptyLine() {
        assertBusiness(() -> create(List.of()), ApprovalErrorCode.APPROVAL_LINE_INVALID);
    }

    @Test
    @DisplayName("결재자가 4명 이상이면 생성할 수 없다")
    void create_tooManyApprovers() {
        assertBusiness(() -> create(List.of("a", "b", "c", "d")),
                ApprovalErrorCode.APPROVAL_LINE_INVALID);
    }

    @Test
    @DisplayName("단일 결재자가 승인하면 즉시 APPROVED")
    void singleApprove() {
        ApprovalRequest r = create(List.of("a"));

        r.decide("a", ApprovalDecisionKind.APPROVE, null, NOW, stepIds);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);
        assertThat(r.getDecidedAt()).isEqualTo(NOW);
        assertThat(r.getApprovalHistory()).hasSize(1);
    }

    @Test
    @DisplayName("2단계 결재선은 순서대로 승인하면 APPROVED")
    void twoStepApprove() {
        ApprovalRequest r = create(List.of("a", "b"));

        r.decide("a", ApprovalDecisionKind.APPROVE, "ok", NOW, stepIds);
        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.IN_REVIEW_2);

        r.decide("b", ApprovalDecisionKind.APPROVE, null, NOW, stepIds);
        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);
    }

    @Test
    @DisplayName("현재 순서의 결재자가 아니면 거부된다")
    void approverMismatch() {
        ApprovalRequest r = create(List.of("a", "b"));

        assertBusiness(() -> r.decide("b", ApprovalDecisionKind.APPROVE, null, NOW, stepIds),
                ApprovalErrorCode.APPROVER_MISMATCH);
    }

    @Test
    @DisplayName("반려하면 즉시 REJECTED 이고 사유가 기록된다")
    void reject() {
        ApprovalRequest r = create(List.of("a", "b"));

        r.decide("a", ApprovalDecisionKind.REJECT, "사유 있음", NOW, stepIds);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.REJECTED);
        assertThat(r.getRejectionReason()).isEqualTo("사유 있음");
    }

    @Test
    @DisplayName("반려 사유가 없으면 거부된다")
    void rejectRequiresReason() {
        ApprovalRequest r = create(List.of("a"));

        assertBusiness(() -> r.decide("a", ApprovalDecisionKind.REJECT, "  ", NOW, stepIds),
                ApprovalErrorCode.APPROVAL_DECISION_REASON_REQUIRED);
    }

    @Test
    @DisplayName("종료된 요청은 더 결재할 수 없다")
    void decideOnTerminal() {
        ApprovalRequest r = create(List.of("a"));
        r.decide("a", ApprovalDecisionKind.APPROVE, null, NOW, stepIds);

        assertBusiness(() -> r.decide("a", ApprovalDecisionKind.APPROVE, null, NOW, stepIds),
                ApprovalErrorCode.APPROVAL_REQUEST_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("신청자는 진행 중 요청을 회수할 수 있다")
    void withdraw() {
        ApprovalRequest r = create(List.of("a"));

        r.withdraw("requester", NOW);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.CANCELLED);
    }

    @Test
    @DisplayName("신청자가 아니면 회수할 수 없다")
    void withdraw_notRequester() {
        ApprovalRequest r = create(List.of("a"));

        assertBusiness(() -> r.withdraw("someone", NOW),
                ApprovalErrorCode.APPROVAL_REQUEST_FORBIDDEN);
    }

    @Test
    @DisplayName("승인된 요청을 취소하면 wasApproved=true")
    void cancelApproved() {
        ApprovalRequest r = create(List.of("a"));
        r.decide("a", ApprovalDecisionKind.APPROVE, null, NOW, stepIds);

        boolean wasApproved = r.cancel(NOW, "관리자 취소");

        assertThat(wasApproved).isTrue();
        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.CANCELLED);
        assertThat(r.getRejectionReason()).isEqualTo("관리자 취소");
    }

    @Test
    @DisplayName("이미 취소/반려된 요청은 다시 취소할 수 없다")
    void cancelTerminal() {
        ApprovalRequest r = create(List.of("a"));
        r.decide("a", ApprovalDecisionKind.REJECT, "반려", NOW, stepIds);

        assertBusiness(() -> r.cancel(NOW, null),
                ApprovalErrorCode.APPROVAL_REQUEST_INVALID_TRANSITION);
    }
}
