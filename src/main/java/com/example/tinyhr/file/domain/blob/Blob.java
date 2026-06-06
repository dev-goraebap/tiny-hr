package com.example.tinyhr.file.domain.blob;

import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로드된 바이너리 1건의 메타데이터 애그리거트(실제 바이트는 {@code BlobStorage} 가 보관).
 *
 * <p>같은 바이트 재업로드는 {@code checksum}(sha256)으로 재사용(dedupe)한다. 소프트 삭제는
 * {@code revokedAt} 으로 표현한다. (썸네일/변형은 MVP 범위 외)
 */
@Entity
@Table(name = "blob")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Blob {

    private static final Pattern SHA256_HEX = Pattern.compile("^[a-fA-F0-9]{64}$");

    @Id
    @Column(name = "blob_id", length = 36)
    private String id;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    /** sha256 hex(소문자). dedupe 키. */
    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** 신규 Blob 생성. 식별자는 도메인이 발급한다. */
    public static Blob create(
            String storageKey,
            String filename,
            String contentType,
            long byteSize,
            String checksum) {
        if (filename == null || filename.isBlank() || byteSize <= 0
                || checksum == null || !SHA256_HEX.matcher(checksum).matches()) {
            throw new BusinessException(FileErrorCode.BLOB_INVALID);
        }
        Blob b = new Blob();
        b.id = UUID.randomUUID().toString();
        b.storageKey = storageKey;
        b.filename = filename.trim();
        b.contentType = contentType;
        b.byteSize = byteSize;
        b.checksum = checksum.toLowerCase();
        b.revokedAt = null;
        return b;
    }

    public void softDelete(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public boolean isDeleted() {
        return revokedAt != null;
    }
}
