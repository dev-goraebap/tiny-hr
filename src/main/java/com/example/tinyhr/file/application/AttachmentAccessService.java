package com.example.tinyhr.file.application;

import com.example.tinyhr.file.application.spi.AttachmentAccessSpi;
import com.example.tinyhr.file.application.spi.AttachmentAccessSpi.ResourceContext;
import com.example.tinyhr.file.application.spi.AttachmentAccessSpi.UploadContext;
import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.attachment.Attachment;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 첨부 접근 인가 디스패처. ownerType 으로 등록된 정책({@link AttachmentAccessSpi})에 위임하고, 미등록
 * ownerType 은 거부한다(file 은 owner 규칙을 직접 알지 못한다).
 *
 * <p>각 owner BC 가 SPI 를 {@code @Component} 로 두면 스프링이 모든 구현을 {@code List} 로 주입한다.
 * 별도 레지스트리 없이 여기서 ownerType 으로 색인한다(같은 타입 중복 등록은 부팅 시 실패).
 */
@Service
public class AttachmentAccessService {

    private final Map<AttachmentOwnerType, AttachmentAccessSpi> byType;

    public AttachmentAccessService(List<AttachmentAccessSpi> spis) {
        Map<AttachmentOwnerType, AttachmentAccessSpi> map =
                new EnumMap<>(AttachmentOwnerType.class);
        for (AttachmentAccessSpi spi : spis) {
            AttachmentAccessSpi prev = map.putIfAbsent(spi.ownerType(), spi);
            if (prev != null) {
                throw new IllegalStateException(
                        "AttachmentAccessSpi 가 ownerType=" + spi.ownerType() + " 에 중복 등록되었습니다");
            }
        }
        this.byType = map;
    }

    public boolean canUpload(String actorId, AttachmentOwnerType ownerType, String ownerId) {
        AttachmentAccessSpi spi = byType.get(ownerType);
        return spi != null && spi.canUpload(new UploadContext(ownerId, actorId));
    }

    public boolean canRead(String actorId, Attachment attachment) {
        AttachmentAccessSpi spi = byType.get(attachment.getOwnerType());
        return spi != null && spi.canRead(resourceContext(actorId, attachment));
    }

    public boolean canDelete(String actorId, Attachment attachment) {
        AttachmentAccessSpi spi = byType.get(attachment.getOwnerType());
        return spi != null && spi.canDelete(resourceContext(actorId, attachment));
    }

    private static ResourceContext resourceContext(String actorId, Attachment a) {
        return new ResourceContext(a.getOwnerId(), actorId, a.getCreatedBy());
    }
}
