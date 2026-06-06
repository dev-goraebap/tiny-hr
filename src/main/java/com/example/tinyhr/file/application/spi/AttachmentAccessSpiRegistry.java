package com.example.tinyhr.file.application.spi;

import com.example.tinyhr.file.domain.AttachmentOwnerType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * ownerType → 접근 정책 레지스트리. 스프링이 모든 {@link AttachmentAccessSpi} 빈을 주입하면 색인한다.
 * 미등록 ownerType 은 {@link #find} 가 empty 를 돌려주고, 디스패처가 기본 거부로 처리한다.
 */
@Component
public class AttachmentAccessSpiRegistry {

    private final Map<AttachmentOwnerType, AttachmentAccessSpi> byType =
            new EnumMap<>(AttachmentOwnerType.class);

    public AttachmentAccessSpiRegistry(List<AttachmentAccessSpi> spis) {
        for (AttachmentAccessSpi spi : spis) {
            AttachmentAccessSpi prev = byType.putIfAbsent(spi.ownerType(), spi);
            if (prev != null) {
                throw new IllegalStateException(
                        "AttachmentAccessSpi 가 ownerType=" + spi.ownerType() + " 에 중복 등록되었습니다");
            }
        }
    }

    public Optional<AttachmentAccessSpi> find(AttachmentOwnerType ownerType) {
        return Optional.ofNullable(byType.get(ownerType));
    }
}
