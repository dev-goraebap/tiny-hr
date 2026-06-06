package com.example.tinyhr.approval.application;

import com.example.tinyhr.approval.domain.ApprovalDecisionKind;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.approval.domain.request.ApprovalRequest;
import com.example.tinyhr.approval.domain.request.ApprovalRequestRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * approval 컨텍스트가 외부에 공개하는 Open Host Service.
 *
 * <p>신청 컨텍스트(vacation·overtime 등)가 결재 애그리거트를 직접 만지지 않고 결재를 신청·확인한다.
 * 결재선 구성·자동승인 규칙은 approval 이 소유하고, 확정 후속은 DIP(SPI)로 역전된다. 호출자는 자기
 * detail 저장과 같은 트랜잭션 안에서 호출한다.
 */
@Service
@Transactional
public class ApprovalOpenHostService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalService approvalService;

    public ApprovalOpenHostService(
            ApprovalRequestRepository approvalRequestRepository, ApprovalService approvalService) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalService = approvalService;
    }

    /**
     * 결재 신청을 생성·저장한다. 요청자는 결재선에서 제외하며(본인 결재 불가), 남는 결재자가 없으면
     * autoApproved=true 로 표시한다(실제 자동 확정은 detail 저장 후 {@link #finalizeAutoApproval}).
     *
     * @param requestId 호출자가 자기 detail 과 연결할 식별자
     */
    public ApprovalSubmissionResult submit(
            String requestId,
            String requesterId,
            ApprovalRequestKind kind,
            List<String> approvers) {
        List<String> others = approvers.stream().filter(id -> !id.equals(requesterId)).toList();
        boolean autoApproved = others.isEmpty();
        List<String> line = autoApproved ? List.of(requesterId) : others;

        ApprovalRequest request =
                ApprovalRequest.create(requestId, requesterId, kind, line, Instant.now());
        approvalRequestRepository.save(request);

        return new ApprovalSubmissionResult(autoApproved, line);
    }

    /**
     * 결재자 없는 신청을 본인 승인으로 즉시 확정한다(detail 저장 이후, submit 이 autoApproved=true 일 때만).
     */
    public void finalizeAutoApproval(String requestId, String requesterId) {
        approvalService.decide(
                requestId, requesterId, ApprovalDecisionKind.APPROVE,
                "자동 승인 — 결재 가능한 상급자 없음", null);
    }

    /** 신청이 최종 승인됐는지. 미존재면 false. */
    @Transactional(readOnly = true)
    public boolean isApproved(String requestId) {
        return approvalRequestRepository.findById(requestId)
                .map(r -> r.getStatus() == ApprovalRequestStatus.APPROVED)
                .orElse(false);
    }

    /** 결재 제출 결과. */
    public record ApprovalSubmissionResult(boolean autoApproved, List<String> approvers) {}
}
