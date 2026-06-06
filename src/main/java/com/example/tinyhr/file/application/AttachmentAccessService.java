package com.example.tinyhr.file.application;

import com.example.tinyhr.file.application.spi.AttachmentAccessSpi.ResourceContext;
import com.example.tinyhr.file.application.spi.AttachmentAccessSpi.UploadContext;
import com.example.tinyhr.file.application.spi.AttachmentAccessSpiRegistry;
import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.attachment.Attachment;
import org.springframework.stereotype.Service;

/**
 * 첨부 접근 인가 디스패처. ownerType 으로 등록된 정책(AttachmentAccessSpi)에 위임하고, 미등록
 * ownerType 은 거부한다(file 은 owner 규칙을 직접 알지 못한다).
 */
@Service
public class AttachmentAccessService {

    private final AttachmentAccessSpiRegistry registry;

    public AttachmentAccessService(AttachmentAccessSpiRegistry registry) {
        this.registry = registry;
    }

    public boolean canUpload(String actorId, AttachmentOwnerType ownerType, String ownerId) {
        return registry.find(ownerType)
                .map(spi -> spi.canUpload(new UploadContext(ownerId, actorId)))
                .orElse(false);
    }

    public boolean canRead(String actorId, Attachment attachment) {
        return registry.find(attachment.getOwnerType())
                .map(spi -> spi.canRead(resourceContext(actorId, attachment)))
                .orElse(false);
    }

    public boolean canDelete(String actorId, Attachment attachment) {
        return registry.find(attachment.getOwnerType())
                .map(spi -> spi.canDelete(resourceContext(actorId, attachment)))
                .orElse(false);
    }

    private static ResourceContext resourceContext(String actorId, Attachment a) {
        return new ResourceContext(a.getOwnerId(), actorId, a.getCreatedBy());
    }
}
