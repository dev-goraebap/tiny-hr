package com.example.tinyhr.file.application;

import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.file.domain.attachment.Attachment;
import com.example.tinyhr.file.domain.attachment.AttachmentRepository;
import com.example.tinyhr.file.domain.blob.Blob;
import com.example.tinyhr.file.domain.blob.BlobRepository;
import com.example.tinyhr.file.domain.blob.BlobStorage;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * file BC 애플리케이션 서비스 — 업로드·다운로드·삭제. 접근 권한은 owner 별 SPI 에 위임한다.
 *
 * <p>업로드는 체크섬으로 같은 바이트를 재사용(dedupe)하고, 첨부 링크는 매 호출마다 새로 만든다.
 *
 * @actor 일반사원(권한 통과 시), 관리자
 */
@Service
@Transactional
public class FileService {

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10MB

    private final AttachmentRepository attachmentRepository;
    private final BlobRepository blobRepository;
    private final BlobStorage blobStorage;
    private final AttachmentAccessService accessService;

    public FileService(
            AttachmentRepository attachmentRepository,
            BlobRepository blobRepository,
            BlobStorage blobStorage,
            AttachmentAccessService accessService) {
        this.attachmentRepository = attachmentRepository;
        this.blobRepository = blobRepository;
        this.blobStorage = blobStorage;
        this.accessService = accessService;
    }

    /** 파일 1건을 업로드한다(권한 검사 → blob dedupe → attachment 링크 생성). */
    public UploadResult upload(
            String actorId,
            String filename,
            String contentType,
            byte[] body,
            AttachmentOwnerType ownerType,
            String ownerId,
            String name,
            int position,
            String note) {
        if (body == null || body.length == 0) {
            throw new BusinessException(FileErrorCode.EMPTY_FILE);
        }
        if (body.length > MAX_BYTES) {
            throw new BusinessException(FileErrorCode.FILE_TOO_LARGE);
        }
        if (!accessService.canUpload(actorId, ownerType, ownerId)) {
            throw new BusinessException(FileErrorCode.ATTACHMENT_UPLOAD_FORBIDDEN);
        }

        String checksum = sha256Hex(body);
        Blob blob = blobRepository.findByChecksum(checksum)
                .filter(b -> !b.isDeleted())
                .orElseGet(() -> storeNewBlob(ownerType, filename, contentType, body, checksum));

        Attachment attachment = Attachment.create(
                blob.getId(), ownerType, ownerId, name, position, note, actorId);
        attachmentRepository.save(attachment);
        return new UploadResult(attachment.getId(), blob.getId());
    }

    /** 첨부 다운로드(권한 검사 후 메타 + 바이트). */
    @Transactional(readOnly = true)
    public Download get(String actorId, String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new BusinessException(FileErrorCode.ATTACHMENT_NOT_FOUND));
        if (!accessService.canRead(actorId, attachment)) {
            throw new BusinessException(FileErrorCode.ATTACHMENT_READ_FORBIDDEN);
        }
        Blob blob = blobRepository.findById(attachment.getBlobId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new BusinessException(FileErrorCode.BLOB_NOT_FOUND));
        byte[] content = blobStorage.load(blob.getStorageKey());
        return new Download(blob.getFilename(), blob.getContentType(), content);
    }

    /** 첨부 소프트 삭제(권한 검사 후). */
    public void delete(String actorId, String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new BusinessException(FileErrorCode.ATTACHMENT_NOT_FOUND));
        if (!accessService.canDelete(actorId, attachment)) {
            throw new BusinessException(FileErrorCode.ATTACHMENT_DELETE_FORBIDDEN);
        }
        attachment.softDelete(Instant.now());
        attachmentRepository.save(attachment);
    }

    private Blob storeNewBlob(
            AttachmentOwnerType ownerType,
            String filename,
            String contentType,
            byte[] body,
            String checksum) {
        String storageKey = ownerType.name().toLowerCase() + "/" + UUID.randomUUID();
        Blob blob = Blob.create(storageKey, filename, contentType, body.length, checksum);
        blobStorage.store(blob.getStorageKey(), body);
        blobRepository.save(blob);
        return blob;
    }

    private static String sha256Hex(byte[] body) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(body);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** 업로드 결과. */
    public record UploadResult(String attachmentId, String blobId) {}

    /** 다운로드 결과(메타 + 바이트). */
    public record Download(String filename, String contentType, byte[] content) {}
}
