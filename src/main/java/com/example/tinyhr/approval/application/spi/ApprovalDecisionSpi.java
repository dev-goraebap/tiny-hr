package com.example.tinyhr.approval.application.spi;

import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import java.time.LocalDate;

/**
 * 결재 확정 후속 처리 포트(허브 → 도메인 역방향 협력).
 *
 * <p>approval(허브)이 <b>소유</b>하는 인터페이스다. 각 신청 도메인(vacation·overtime 등)이 자기
 * 모듈에서 이 인터페이스를 구현하고 {@link ApprovalDecisionSpiRegistry} 에 self-register 한다.
 * 소스 의존은 항상 <b>도메인 → approval</b> 단방향이며, 런타임 제어 흐름만 레지스트리를 통해
 * approval → 도메인으로 흐른다(의존성 역전).
 */
public interface ApprovalDecisionSpi {

    /** 디스패치 키. */
    ApprovalRequestKind kind();

    /** 취소 가능 여부 검사(컨텍스트 특화 invariant). 실패 시 도메인 예외를 던진다. */
    void assertCancellable(ApprovalDecisionContext request, LocalDate today);

    /** 최종 승인 시 후속 처리(같은 트랜잭션 — 예외 시 롤백). */
    void onApproved(ApprovalDecisionContext request);

    /** 최종 반려 시 후속 처리. */
    void onRejected(ApprovalDecisionContext request);

    /** 취소 시 후속 처리. wasApproved=true 면 승인 후 취소 — 보상 필요. */
    void onCancelled(ApprovalDecisionContext request, boolean wasApproved);

    /** 회수(승인 전 본인 거둠) 시 후속 처리. */
    void onWithdrawn(ApprovalDecisionContext request);
}
