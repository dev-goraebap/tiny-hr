package com.example.tinyhr.approval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.approval.application.spi.ApprovalDecisionSpi;
import com.example.tinyhr.approval.application.spi.ApprovalDecisionSpiRegistry;
import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.approval.domain.request.ApprovalRequest;
import com.example.tinyhr.approval.domain.request.ApprovalRequestRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock ApprovalDecisionSpiRegistry registry;
    @Mock ApprovalDecisionSpi spi;

    @InjectMocks ApprovalService approvalService;

    private static void assertBusiness(ThrowingCallable callable, ApprovalErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static ApprovalRequest singleStep() {
        return ApprovalRequest.create(
                "r", "requester", ApprovalRequestKind.LEAVE, List.of("a"), Instant.now());
    }

    @Test
    @DisplayName("최종 승인 시 SPI onApproved 로 디스패치한다")
    void decide_dispatchesOnApproved() {
        given(approvalRequestRepository.findById("r")).willReturn(Optional.of(singleStep()));
        given(registry.find(ApprovalRequestKind.LEAVE)).willReturn(Optional.of(spi));

        approvalService.decide("r", "a", ApprovalDecisionKind.APPROVE, null, null);

        then(spi).should().onApproved(any());
        then(approvalRequestRepository).should().save(any(ApprovalRequest.class));
    }

    @Test
    @DisplayName("등록된 SPI 가 없으면 콜백 없이 상태만 전이한다")
    void decide_noSpi() {
        ApprovalRequest request = singleStep();
        given(approvalRequestRepository.findById("r")).willReturn(Optional.of(request));
        given(registry.find(ApprovalRequestKind.LEAVE)).willReturn(Optional.empty());

        approvalService.decide("r", "a", ApprovalDecisionKind.APPROVE, null, null);

        assertThat(request.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);
    }

    @Test
    @DisplayName("없는 요청은 결재할 수 없다")
    void decide_notFound() {
        given(approvalRequestRepository.findById("none")).willReturn(Optional.empty());

        assertBusiness(
                () -> approvalService.decide("none", "a", ApprovalDecisionKind.APPROVE, null, null),
                ApprovalErrorCode.APPROVAL_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 신청을 취소한다")
    void cancel() {
        ApprovalRequest request = singleStep();
        given(approvalRequestRepository.findById("r")).willReturn(Optional.of(request));
        given(registry.find(ApprovalRequestKind.LEAVE)).willReturn(Optional.empty());

        approvalService.cancel("r", "requester");

        assertThat(request.getStatus()).isEqualTo(ApprovalRequestStatus.CANCELLED);
    }

    @Test
    @DisplayName("신청자가 아니면 취소할 수 없다")
    void cancel_forbidden() {
        given(approvalRequestRepository.findById("r")).willReturn(Optional.of(singleStep()));

        assertBusiness(() -> approvalService.cancel("r", "other"),
                ApprovalErrorCode.APPROVAL_REQUEST_FORBIDDEN);
        then(approvalRequestRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("진행 중 요청을 회수한다")
    void withdraw() {
        ApprovalRequest request = singleStep();
        given(approvalRequestRepository.findById("r")).willReturn(Optional.of(request));
        given(registry.find(ApprovalRequestKind.LEAVE)).willReturn(Optional.empty());

        approvalService.withdraw("r", "requester");

        assertThat(request.getStatus()).isEqualTo(ApprovalRequestStatus.CANCELLED);
    }
}
