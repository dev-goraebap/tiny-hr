package com.example.tinyhr.file.application.spi;

import com.example.tinyhr.file.domain.AttachmentOwnerType;

/**
 * 첨부 owner 별 접근 인가 전략(DIP 포트). file 이 <b>소유</b>하는 인터페이스다.
 *
 * <p>file 은 owner(휴가신청·프로필 등) 도메인 규칙을 알지 못한다. 각 owner BC 가 이 인터페이스를
 * 구현(@Component)하면 스프링이 모든 구현을 {@code List} 로 모아 {@code AttachmentAccessService} 에
 * 주입하고, file 은 ownerType 으로 디스패치만 한다. 의존은 owner BC → file 단방향이며, file 은
 * rbac·타 BC 테이블을 직접 보지 않는다(의존성 역전).
 */
public interface AttachmentAccessSpi {

    AttachmentOwnerType ownerType();

    /** 업로드 가능 여부(첨부 생성 전이라 owner/actor 만 안다). */
    boolean canUpload(UploadContext ctx);

    /** 조회(다운로드) 가능 여부. */
    boolean canRead(ResourceContext ctx);

    /** 삭제 가능 여부. */
    boolean canDelete(ResourceContext ctx);

    record UploadContext(String ownerId, String actorId) {}

    /** @param createdBy 첨부 업로더(없으면 null) — "본인 업로드" 판정에 사용 */
    record ResourceContext(String ownerId, String actorId, String createdBy) {}
}
