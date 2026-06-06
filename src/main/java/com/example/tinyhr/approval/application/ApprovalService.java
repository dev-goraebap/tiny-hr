package com.example.tinyhr.approval.application;

import com.example.tinyhr.approval.application.spi.ApprovalDecisionContext;
import com.example.tinyhr.approval.application.spi.ApprovalDecisionSpi;
import com.example.tinyhr.approval.application.spi.ApprovalDecisionSpiRegistry;
import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.approval.domain.request.ApprovalRequest;
import com.example.tinyhr.approval.domain.request.ApprovalRequestRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재 진행(승인·반려·취소·회수)을 결재 종류 무관하게 처리하고, 확정 후속을 SPI 로 디스패치한다.
 *
 * <p>확정 후속(onApproved 등)은 같은 트랜잭션에서 실행된다(예외 시 롤백). 소비 BC 의 SPI 가 아직
 * 등록되지 않았으면 콜백 없이 상태 전이만 한다(MVP — vacation 이식 시 콜백이 붙는다).
 *
 * @actor 결재자, 신청자, 관리자
 */
@Service
@Transactional
public class ApprovalService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDecisionSpiRegistry registry;

    public ApprovalService(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalDecisionSpiRegistry registry) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.registry = registry;
    }

    /** 현재 차수 결재자가 승인 또는 반려한다. 최종 확정 시 SPI 후속 처리. */
    public void decide(
            String requestId,
            String approverId,
            ApprovalDecisionKind action,
            String comment,
            String reason) {
        ApprovalRequest request = load(requestId);
        String text = action == ApprovalDecisionKind.REJECT ? reason : comment;
        request.decide(approverId, action, text, Instant.now(), () -> UUID.randomUUID().toString());
        approvalRequestRepository.save(request);

        if (request.getStatus() == ApprovalRequestStatus.APPROVED) {
            spi(request).ifPresent(s -> s.onApproved(toContext(request)));
        } else if (request.getStatus() == ApprovalRequestStatus.REJECTED) {
            spi(request).ifPresent(s -> s.onRejected(toContext(request)));
        }
    }

    /** 결재 진행 중인 본인 신청을 회수한다. */
    public void withdraw(String requestId, String requesterId) {
        ApprovalRequest request = load(requestId);
        request.withdraw(requesterId, Instant.now());
        approvalRequestRepository.save(request);
        spi(request).ifPresent(s -> s.onWithdrawn(toContext(request)));
    }

    /** 본인 신청을 취소한다(컨텍스트 특화 취소 가능 검사 후). */
    public void cancel(String requestId, String requesterId) {
        ApprovalRequest request = load(requestId);
        if (!request.getRequesterId().equals(requesterId)) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_REQUEST_FORBIDDEN);
        }
        spi(request).ifPresent(s -> s.assertCancellable(toContext(request), LocalDate.now(KST)));

        boolean wasApproved = request.cancel(Instant.now(), null);
        approvalRequestRepository.save(request);
        spi(request).ifPresent(s -> s.onCancelled(toContext(request), wasApproved));
    }

    /** 관리자가 본인 검증·도래 invariant 를 우회해 강제 취소한다(사유 필수). */
    public void cancelAsAdmin(String requestId, String reason) {
        ApprovalRequest request = load(requestId);
        boolean wasApproved = request.cancel(Instant.now(), reason);
        approvalRequestRepository.save(request);
        spi(request).ifPresent(s -> s.onCancelled(toContext(request), wasApproved));
    }

    private ApprovalRequest load(String requestId) {
        return approvalRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new BusinessException(ApprovalErrorCode.APPROVAL_REQUEST_NOT_FOUND));
    }

    private Optional<ApprovalDecisionSpi> spi(ApprovalRequest request) {
        return registry.find(request.getKind());
    }

    private static ApprovalDecisionContext toContext(ApprovalRequest request) {
        return new ApprovalDecisionContext(
                request.getId(),
                request.getRequesterId(),
                request.getKind(),
                request.getStatus(),
                request.getApprovalLine(),
                request.getRejectionReason(),
                request.lastDecidedOrder());
    }
}
