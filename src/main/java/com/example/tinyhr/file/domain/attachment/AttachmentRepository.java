package com.example.tinyhr.file.domain.attachment;

import com.example.tinyhr.file.domain.AttachmentOwnerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 첨부 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    /** 특정 owner 의 활성 첨부 목록(정렬순). */
    List<Attachment> findByOwnerTypeAndOwnerIdAndRevokedAtIsNullOrderByPositionAsc(
            AttachmentOwnerType ownerType, String ownerId);
}
