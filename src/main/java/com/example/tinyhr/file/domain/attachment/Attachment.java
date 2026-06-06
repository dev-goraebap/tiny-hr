package com.example.tinyhr.file.domain.attachment;

import com.example.tinyhr.file.domain.AttachmentOwnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 첨부 애그리거트 — blob ↔ owner 다형 링크. 같은 blob 이 여러 owner 에 붙을 수 있다.
 *
 * <p>접근 권한 판정은 owner 도메인 규칙(AttachmentAccessSpi)에 위임한다. 회수(소프트 삭제)는
 * {@code revokedAt} 으로 표현한다. {@code createdBy}(업로더)는 "본인 업로드" 판정에 쓰인다.
 */
@Entity
@Table(name = "attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

    @Id
    @Column(name = "attachment_id", length = 36)
    private String id;

    @Column(name = "blob_id", nullable = false, length = 36)
    private String blobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 40)
    private AttachmentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    /** 첨부 역할 라벨(예: profile/evidence). */
    @Column(name = "name")
    private String name;

    /** owner 내 정렬 순서. */
    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "note", length = 500)
    private String note;

    /** 업로더 계정 id. 시스템 자동 첨부 시 null. */
    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** 신규 첨부 링크 생성. 식별자는 도메인이 발급한다. */
    public static Attachment create(
            String blobId,
            AttachmentOwnerType ownerType,
            String ownerId,
            String name,
            int position,
            String note,
            String uploaderId) {
        Attachment a = new Attachment();
        a.id = UUID.randomUUID().toString();
        a.blobId = blobId;
        a.ownerType = ownerType;
        a.ownerId = ownerId;
        a.name = name;
        a.position = position;
        a.note = note;
        a.createdBy = uploaderId;
        a.revokedAt = null;
        return a;
    }

    public void softDelete(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    /** 사전 업로드 첨부의 owner 를 실제 엔티티 id 로 이관(ownerType 은 불변). */
    public void transferOwnerTo(String newOwnerId) {
        this.ownerId = newOwnerId;
    }

    public boolean isDeleted() {
        return revokedAt != null;
    }
}
