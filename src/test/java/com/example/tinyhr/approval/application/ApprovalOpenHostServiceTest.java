package com.example.tinyhr.approval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.tinyhr.approval.application.ApprovalOpenHostService.ApprovalSubmissionResult;
import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.approval.domain.request.ApprovalRequest;
import com.example.tinyhr.approval.domain.request.ApprovalRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalOpenHostServiceTest {

    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock ApprovalService approvalService;

    @InjectMocks ApprovalOpenHostService ohs;

    @Test
    @DisplayName("제출 시 신청자를 결재선에서 제외한다")
    void submit_excludesRequester() {
        ApprovalSubmissionResult result = ohs.submit(
                "r", "req", ApprovalRequestKind.LEAVE, List.of("req", "a", "b"));

        assertThat(result.autoApproved()).isFalse();
        assertThat(result.approvers()).containsExactly("a", "b");
        then(approvalRequestRepository).should().save(any(ApprovalRequest.class));
    }

    @Test
    @DisplayName("남는 결재자가 없으면 autoApproved=true")
    void submit_autoApproved() {
        ApprovalSubmissionResult result = ohs.submit(
                "r", "req", ApprovalRequestKind.LEAVE, List.of("req"));

        assertThat(result.autoApproved()).isTrue();
        assertThat(result.approvers()).containsExactly("req");
    }

    @Test
    @DisplayName("자동 승인 확정은 본인 승인으로 decide 를 호출한다")
    void finalizeAutoApproval() {
        ohs.finalizeAutoApproval("r", "req");

        then(approvalService).should().decide(
                eq("r"), eq("req"), eq(ApprovalDecisionKind.APPROVE), any(), isNull());
    }

    @Test
    @DisplayName("최종 승인된 요청이면 isApproved=true")
    void isApproved() {
        ApprovalRequest approved =
                ApprovalRequest.create("r", "req", ApprovalRequestKind.LEAVE, List.of("a"), Instant.now());
        approved.decide("a", ApprovalDecisionKind.APPROVE, null, Instant.now(), () -> "s");
        given(approvalRequestRepository.findById("r")).willReturn(Optional.of(approved));

        assertThat(ohs.isApproved("r")).isTrue();
        assertThat(approved.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);
    }

    @Test
    @DisplayName("없는 요청은 isApproved=false")
    void isApproved_missing() {
        given(approvalRequestRepository.findById("none")).willReturn(Optional.empty());

        assertThat(ohs.isApproved("none")).isFalse();
    }
}
