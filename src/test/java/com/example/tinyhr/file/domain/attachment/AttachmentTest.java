package com.example.tinyhr.file.domain.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tinyhr.file.domain.AttachmentOwnerType;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttachmentTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static Attachment newAttachment() {
        return Attachment.create("blob-1", AttachmentOwnerType.EMPLOYEE_PROFILE,
                "emp-1", "profile", 0, null, "emp-1");
    }

    @Test
    @DisplayName("첨부 링크를 생성한다")
    void create() {
        Attachment a = newAttachment();

        assertThat(a.getId()).isNotBlank();
        assertThat(a.getBlobId()).isEqualTo("blob-1");
        assertThat(a.getOwnerType()).isEqualTo(AttachmentOwnerType.EMPLOYEE_PROFILE);
        assertThat(a.getCreatedBy()).isEqualTo("emp-1");
        assertThat(a.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("소프트 삭제는 멱등이다")
    void softDelete() {
        Attachment a = newAttachment();

        a.softDelete(T0);
        a.softDelete(T0.plusSeconds(10));

        assertThat(a.isDeleted()).isTrue();
        assertThat(a.getRevokedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("owner 를 이관할 수 있다")
    void transferOwnerTo() {
        Attachment a = newAttachment();

        a.transferOwnerTo("request-99");

        assertThat(a.getOwnerId()).isEqualTo("request-99");
        assertThat(a.getOwnerType()).isEqualTo(AttachmentOwnerType.EMPLOYEE_PROFILE);
    }
}
