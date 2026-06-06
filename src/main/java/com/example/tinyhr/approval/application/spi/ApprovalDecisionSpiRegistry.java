package com.example.tinyhr.approval.application.spi;

import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * kind 별 결재 후속 처리 SPI 레지스트리.
 *
 * <p>스프링이 컨텍스트의 모든 {@link ApprovalDecisionSpi} 빈을 주입하면 kind 로 색인한다. 소비 BC
 * 가 아직 없으면 비어 있고, {@link #find}는 {@code Optional.empty()}를 돌려준다(워크플로는 콜백 없이
 * 그대로 진행). 같은 kind 가 둘 이상 등록되면 부팅 시 실패시킨다.
 */
@Component
public class ApprovalDecisionSpiRegistry {

    private final Map<ApprovalRequestKind, ApprovalDecisionSpi> byKind =
            new EnumMap<>(ApprovalRequestKind.class);

    public ApprovalDecisionSpiRegistry(List<ApprovalDecisionSpi> spis) {
        for (ApprovalDecisionSpi spi : spis) {
            ApprovalDecisionSpi prev = byKind.putIfAbsent(spi.kind(), spi);
            if (prev != null) {
                throw new IllegalStateException(
                        "ApprovalDecisionSpi 가 kind=" + spi.kind() + " 에 중복 등록되었습니다");
            }
        }
    }

    /** 해당 kind 의 SPI. 없으면 empty(소비 BC 미등록). */
    public Optional<ApprovalDecisionSpi> find(ApprovalRequestKind kind) {
        return Optional.ofNullable(byKind.get(kind));
    }
}
